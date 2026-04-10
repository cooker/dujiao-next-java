package com.dujiao.api.service;

import com.dujiao.api.auth.AuthConstants;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.EmailVerificationCodeEntity;
import com.dujiao.api.repository.EmailVerificationCodeRepository;
import com.dujiao.api.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_TTL_MINUTES = 15;
    private static final int RATE_LIMIT_SECONDS = 60;

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMailHelper authMailHelper;
    private final SettingsService settingsService;

    public EmailVerificationService(
            EmailVerificationCodeRepository emailVerificationCodeRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthMailHelper authMailHelper,
            SettingsService settingsService) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authMailHelper = authMailHelper;
        this.settingsService = settingsService;
    }

    @Transactional
    public void sendVerifyCode(String email, String purpose) {
        if (!settingsService.emailVerificationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "email_verification_disabled");
        }
        String em = normalizeEmail(email);
        String p = purpose.toLowerCase(Locale.ROOT).trim();
        if (!AuthConstants.PURPOSE_REGISTER.equals(p)
                && !AuthConstants.PURPOSE_RESET_PASSWORD.equals(p)
                && !AuthConstants.PURPOSE_CHANGE_EMAIL_OLD.equals(p)
                && !AuthConstants.PURPOSE_CHANGE_EMAIL_NEW.equals(p)
                && !AuthConstants.PURPOSE_TELEGRAM_BIND.equals(p)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "verify_purpose_invalid");
        }
        if (AuthConstants.PURPOSE_REGISTER.equals(p)) {
            if (!settingsService.registrationEnabled()) {
                throw new BusinessException(ResponseCodes.FORBIDDEN, "registration_disabled");
            }
            if (userAccountRepository.existsByEmail(em)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_exists");
            }
        } else if (AuthConstants.PURPOSE_RESET_PASSWORD.equals(p)
                || AuthConstants.PURPOSE_CHANGE_EMAIL_OLD.equals(p)) {
            if (!userAccountRepository.existsByEmail(em)) {
                throw new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found");
            }
        } else if (AuthConstants.PURPOSE_TELEGRAM_BIND.equals(p)) {
            if (!userAccountRepository.existsByEmail(em)) {
                throw new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found");
            }
        } else {
            if (userAccountRepository.existsByEmail(em)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_exists");
            }
        }

        emailVerificationCodeRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(em, p)
                .ifPresent(
                        last -> {
                            if (last.getCreatedAt().plusSeconds(RATE_LIMIT_SECONDS).isAfter(Instant.now())) {
                                throw new BusinessException(ResponseCodes.TOO_MANY_REQUESTS, "verify_code_too_frequent");
                            }
                        });

        emailVerificationCodeRepository.deleteByEmailAndPurpose(em, p);

        String plain = randomDigits(CODE_LENGTH);
        EmailVerificationCodeEntity e = new EmailVerificationCodeEntity();
        e.setEmail(em);
        e.setPurpose(p);
        e.setCodeHash(passwordEncoder.encode(plain));
        e.setExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        emailVerificationCodeRepository.save(e);

        String label =
                switch (p) {
                    case AuthConstants.PURPOSE_REGISTER -> "注册";
                    case AuthConstants.PURPOSE_RESET_PASSWORD -> "重置密码";
                    case AuthConstants.PURPOSE_CHANGE_EMAIL_OLD -> "更换邮箱（验证当前邮箱）";
                    case AuthConstants.PURPOSE_CHANGE_EMAIL_NEW -> "更换邮箱（验证新邮箱）";
                    case AuthConstants.PURPOSE_TELEGRAM_BIND -> "Telegram 渠道绑定";
                    default -> "验证";
                };
        authMailHelper.sendVerificationCode(em, plain, label);
    }

    @Transactional
    public void verifyAndConsumeCode(String email, String purpose, String plainCode) {
        String em = normalizeEmail(email);
        String p = purpose.toLowerCase(Locale.ROOT).trim();
        EmailVerificationCodeEntity e =
                emailVerificationCodeRepository
                        .findTopByEmailAndPurposeOrderByCreatedAtDesc(em, p)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "verify_code_invalid"));
        if (e.getExpiresAt().isBefore(Instant.now())) {
            emailVerificationCodeRepository.delete(e);
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "verify_code_expired");
        }
        if (plainCode == null || !passwordEncoder.matches(plainCode.trim(), e.getCodeHash())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "verify_code_invalid");
        }
        emailVerificationCodeRepository.delete(e);
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }

    private static String randomDigits(int len) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }
}
