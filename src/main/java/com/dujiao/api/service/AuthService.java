package com.dujiao.api.service;

import com.dujiao.api.auth.AuthConstants;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.dto.auth.ForgotPasswordRequest;
import com.dujiao.api.dto.auth.LoginRequest;
import com.dujiao.api.dto.auth.LoginResponse;
import com.dujiao.api.dto.auth.RegisterRequest;
import com.dujiao.api.dto.auth.SendVerifyCodeRequest;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.security.JwtProperties;
import com.dujiao.api.security.JwtService;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final WalletBootstrapService walletBootstrapService;
    private final SettingsService settingsService;
    private final EmailVerificationService emailVerificationService;
    private final MemberLevelRepository memberLevelRepository;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            WalletBootstrapService walletBootstrapService,
            SettingsService settingsService,
            EmailVerificationService emailVerificationService,
            MemberLevelRepository memberLevelRepository) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.walletBootstrapService = walletBootstrapService;
        this.settingsService = settingsService;
        this.emailVerificationService = emailVerificationService;
        this.memberLevelRepository = memberLevelRepository;
    }

    @Transactional
    public void sendVerifyCode(SendVerifyCodeRequest req) {
        emailVerificationService.sendVerifyCode(req.email(), req.purpose());
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (Boolean.FALSE.equals(req.agreeTerms())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "terms_not_accepted");
        }
        if (!settingsService.registrationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "registration_disabled");
        }
        String email = req.email().toLowerCase(Locale.ROOT).trim();
        if (userAccountRepository.existsByEmail(email)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "email_already_registered");
        }
        if (settingsService.emailVerificationEnabled()) {
            String code = req.verifyCode();
            if (code == null || code.isBlank()) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "verify_code_required");
            }
            emailVerificationService.verifyAndConsumeCode(
                    email, AuthConstants.PURPOSE_REGISTER, code);
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setDisplayName(req.displayName() != null ? req.displayName().trim() : "");
        u.setStatus("active");
        u.setEmailVerified(true);
        memberLevelRepository
                .findByDefaultLevelTrue()
                .ifPresent(ml -> u.setMemberLevelId(ml.getId()));
        final UserAccount saved = userAccountRepository.save(u);
        walletBootstrapService.ensureWallet(saved.getId());
        JwtService.IssuedToken issued =
                jwtService.issueUserToken(saved.getId(), jwtProperties.getExpirationMs());
        return new LoginResponse(
                issued.token(),
                issued.expiresAtEpochSeconds(),
                new LoginResponse.UserBrief(saved.getId(), saved.getEmail(), saved.getDisplayName()));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        UserAccount u =
                userAccountRepository
                        .findByEmail(req.email().toLowerCase(Locale.ROOT).trim())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.BAD_REQUEST, "invalid_credentials"));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_credentials");
        }
        if (!"active".equals(u.getStatus())) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "account_disabled");
        }
        if (settingsService.emailVerificationEnabled() && !u.isEmailVerified()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "email_not_verified");
        }
        long expMs =
                Boolean.TRUE.equals(req.rememberMe())
                        ? jwtProperties.getRememberExpirationMs()
                        : jwtProperties.getExpirationMs();
        JwtService.IssuedToken issued = jwtService.issueUserToken(u.getId(), expMs);
        return new LoginResponse(
                issued.token(),
                issued.expiresAtEpochSeconds(),
                new LoginResponse.UserBrief(u.getId(), u.getEmail(), u.getDisplayName()));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        if (!settingsService.emailVerificationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "email_verification_disabled");
        }
        String email = req.email().toLowerCase(Locale.ROOT).trim();
        UserAccount u =
                userAccountRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        emailVerificationService.verifyAndConsumeCode(
                email, AuthConstants.PURPOSE_RESET_PASSWORD, req.verifyCode());
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userAccountRepository.save(u);
    }
}
