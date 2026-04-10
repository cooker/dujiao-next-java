package com.dujiao.api.service;

import com.dujiao.api.auth.AuthConstants;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.dto.user.ChangeEmailRequest;
import com.dujiao.api.repository.UserAccountRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserEmailChangeService {

    private final UserAccountRepository userAccountRepository;
    private final EmailVerificationService emailVerificationService;
    private final SettingsService settingsService;

    public UserEmailChangeService(
            UserAccountRepository userAccountRepository,
            EmailVerificationService emailVerificationService,
            SettingsService settingsService) {
        this.userAccountRepository = userAccountRepository;
        this.emailVerificationService = emailVerificationService;
        this.settingsService = settingsService;
    }

    @Transactional
    public void sendChangeEmailCode(long userId, String kind, String newEmail) {
        if (!settingsService.emailVerificationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "email_verification_disabled");
        }
        UserAccount user =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        String mode = UserProfileService.resolveEmailChangeMode(user);
        String k = kind.toLowerCase(Locale.ROOT).trim();
        if ("old".equals(k)) {
            if (AuthConstants.EMAIL_CHANGE_MODE_BIND_ONLY.equals(mode)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_invalid");
            }
            emailVerificationService.sendVerifyCode(
                    user.getEmail(), AuthConstants.PURPOSE_CHANGE_EMAIL_OLD);
        } else if ("new".equals(k)) {
            if (newEmail == null || newEmail.isBlank()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_invalid");
            }
            String normalized = normalizeEmail(newEmail);
            if (normalized.equals(normalizeEmail(user.getEmail()))) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_invalid");
            }
            if (userAccountRepository.existsByEmail(normalized)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_exists");
            }
            emailVerificationService.sendVerifyCode(
                    normalized, AuthConstants.PURPOSE_CHANGE_EMAIL_NEW);
        } else {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_invalid");
        }
    }

    @Transactional
    public void changeEmail(long userId, ChangeEmailRequest req) {
        if (!settingsService.emailVerificationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "email_verification_disabled");
        }
        UserAccount user =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        String normalized = normalizeEmail(req.newEmail());
        if (normalized.equals(normalizeEmail(user.getEmail()))) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_invalid");
        }
        if (userAccountRepository.existsByEmail(normalized)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_change_exists");
        }
        String mode = UserProfileService.resolveEmailChangeMode(user);
        if (!AuthConstants.EMAIL_CHANGE_MODE_BIND_ONLY.equals(mode)) {
            if (req.oldCode() == null || req.oldCode().isBlank()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "verify_code_invalid");
            }
            emailVerificationService.verifyAndConsumeCode(
                    user.getEmail(), AuthConstants.PURPOSE_CHANGE_EMAIL_OLD, req.oldCode());
        }
        emailVerificationService.verifyAndConsumeCode(
                normalized, AuthConstants.PURPOSE_CHANGE_EMAIL_NEW, req.newCode());
        user.setEmail(normalized);
        user.setEmailVerified(true);
        userAccountRepository.save(user);
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
