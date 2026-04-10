package com.dujiao.api.service;

import com.dujiao.api.auth.AuthConstants;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.domain.UserOAuthIdentityEntity;
import com.dujiao.api.dto.auth.LoginResponse;
import com.dujiao.api.dto.auth.TelegramLoginRequest;
import com.dujiao.api.dto.auth.TelegramMiniAppLoginRequest;
import com.dujiao.api.dto.user.TelegramBindingDto;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.repository.UserOAuthIdentityRepository;
import com.dujiao.api.security.JwtProperties;
import com.dujiao.api.security.JwtService;
import com.dujiao.api.service.settings.TelegramAuthSettings;
import com.dujiao.api.telegram.TelegramIdentityHelper;
import com.dujiao.api.telegram.TelegramWebAuth;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramAuthService {

    private final SettingsService settingsService;
    private final UserAccountRepository userAccountRepository;
    private final UserOAuthIdentityRepository userOAuthIdentityRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final WalletBootstrapService walletBootstrapService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public TelegramAuthService(
            SettingsService settingsService,
            UserAccountRepository userAccountRepository,
            UserOAuthIdentityRepository userOAuthIdentityRepository,
            MemberLevelRepository memberLevelRepository,
            WalletBootstrapService walletBootstrapService,
            JwtService jwtService,
            JwtProperties jwtProperties,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.settingsService = settingsService;
        this.userAccountRepository = userAccountRepository;
        this.userOAuthIdentityRepository = userOAuthIdentityRepository;
        this.memberLevelRepository = memberLevelRepository;
        this.walletBootstrapService = walletBootstrapService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LoginResponse loginWithTelegramWidget(TelegramLoginRequest req) {
        TelegramAuthSettings cfg = settingsService.telegramAuthSettings();
        VerifiedTelegramIdentity v = verifyLoginWidget(req, cfg);
        return loginWithVerified(v);
    }

    @Transactional
    public LoginResponse loginWithMiniApp(TelegramMiniAppLoginRequest req) {
        TelegramAuthSettings cfg = settingsService.telegramAuthSettings();
        VerifiedTelegramIdentity v = verifyMiniAppInitData(req.resolvedInitData(), cfg);
        return loginWithVerified(v);
    }

    @Transactional(readOnly = true)
    public TelegramBindingDto getTelegramBinding(long userId) {
        return userOAuthIdentityRepository
                .findByUserIdAndProvider(userId, AuthConstants.OAUTH_PROVIDER_TELEGRAM)
                .map(TelegramBindingDto::fromEntity)
                .orElseGet(TelegramBindingDto::unbound);
    }

    @Transactional
    public TelegramBindingDto bindTelegram(long userId, TelegramLoginRequest req) {
        TelegramAuthSettings cfg = settingsService.telegramAuthSettings();
        VerifiedTelegramIdentity v = verifyLoginWidget(req, cfg);
        return bindVerifiedToUser(userId, v);
    }

    @Transactional
    public TelegramBindingDto bindTelegramMiniApp(long userId, TelegramMiniAppLoginRequest req) {
        TelegramAuthSettings cfg = settingsService.telegramAuthSettings();
        VerifiedTelegramIdentity v = verifyMiniAppInitData(req.resolvedInitData(), cfg);
        return bindVerifiedToUser(userId, v);
    }

    @Transactional
    public void unbindTelegram(long userId) {
        UserAccount user =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "account_disabled");
        }
        if (TelegramIdentityHelper.isPlaceholderEmail(user.getEmail())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_unbind_requires_email");
        }
        UserOAuthIdentityEntity identity =
                userOAuthIdentityRepository
                        .findByUserIdAndProvider(userId, AuthConstants.OAUTH_PROVIDER_TELEGRAM)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_not_bound"));
        userOAuthIdentityRepository.delete(identity);
    }

    private TelegramBindingDto bindVerifiedToUser(long userId, VerifiedTelegramIdentity verified) {
        UserAccount user =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "account_disabled");
        }
        var occupied =
                userOAuthIdentityRepository.findByProviderAndProviderUserId(
                        AuthConstants.OAUTH_PROVIDER_TELEGRAM, verified.providerUserId());
        if (occupied.isPresent() && !occupied.get().getUserId().equals(userId)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_bind_conflict");
        }
        var current =
                userOAuthIdentityRepository.findByUserIdAndProvider(
                        userId, AuthConstants.OAUTH_PROVIDER_TELEGRAM);
        if (current.isPresent()) {
            if (!current.get().getProviderUserId().equals(verified.providerUserId())) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_already_bound");
            }
            applyTelegramIdentity(verified, current.get());
            UserOAuthIdentityEntity saved = userOAuthIdentityRepository.save(current.get());
            return TelegramBindingDto.fromEntity(saved);
        }
        UserOAuthIdentityEntity identity = new UserOAuthIdentityEntity();
        identity.setUserId(userId);
        identity.setProvider(AuthConstants.OAUTH_PROVIDER_TELEGRAM);
        identity.setProviderUserId(verified.providerUserId());
        identity.setUsername(verified.username());
        identity.setAvatarUrl(verified.avatarUrl());
        try {
            identity = userOAuthIdentityRepository.save(identity);
        } catch (DataIntegrityViolationException e) {
            var row =
                    userOAuthIdentityRepository
                            .findByProviderAndProviderUserId(
                                    AuthConstants.OAUTH_PROVIDER_TELEGRAM, verified.providerUserId())
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ResponseCodes.BAD_REQUEST, "telegram_bind_conflict"));
            if (!row.getUserId().equals(userId)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_bind_conflict");
            }
            applyTelegramIdentity(verified, row);
            identity = userOAuthIdentityRepository.save(row);
        }
        return TelegramBindingDto.fromEntity(identity);
    }

    private LoginResponse loginWithVerified(VerifiedTelegramIdentity verified) {
        var existing =
                userOAuthIdentityRepository.findByProviderAndProviderUserId(
                        AuthConstants.OAUTH_PROVIDER_TELEGRAM, verified.providerUserId());
        UserAccount user;
        if (existing.isPresent()) {
            UserOAuthIdentityEntity identity = existing.get();
            user =
                    userAccountRepository
                            .findById(identity.getUserId())
                            .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
            if (!"active".equals(user.getStatus())) {
                throw new BusinessException(ResponseCodes.FORBIDDEN, "account_disabled");
            }
            applyTelegramIdentity(verified, identity);
            userOAuthIdentityRepository.save(identity);
        } else {
            user = findOrCreateTelegramUser(verified);
            UserOAuthIdentityEntity identity = new UserOAuthIdentityEntity();
            identity.setUserId(user.getId());
            identity.setProvider(AuthConstants.OAUTH_PROVIDER_TELEGRAM);
            identity.setProviderUserId(verified.providerUserId());
            identity.setUsername(verified.username());
            identity.setAvatarUrl(verified.avatarUrl());
            try {
                userOAuthIdentityRepository.save(identity);
            } catch (DataIntegrityViolationException e) {
                var race =
                        userOAuthIdentityRepository
                                .findByProviderAndProviderUserId(
                                        AuthConstants.OAUTH_PROVIDER_TELEGRAM, verified.providerUserId())
                                .orElseThrow(
                                        () ->
                                                new BusinessException(
                                                        ResponseCodes.BAD_REQUEST, "telegram_identity_conflict"));
                user =
                        userAccountRepository
                                .findById(race.getUserId())
                                .orElseThrow(
                                        () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
            }
        }
        JwtService.IssuedToken issued =
                jwtService.issueUserToken(user.getId(), jwtProperties.getExpirationMs());
        return new LoginResponse(
                issued.token(),
                issued.expiresAtEpochSeconds(),
                new LoginResponse.UserBrief(user.getId(), user.getEmail(), user.getDisplayName()));
    }

    private UserAccount findOrCreateTelegramUser(VerifiedTelegramIdentity verified) {
        String email = TelegramIdentityHelper.placeholderEmail(verified.providerUserId());
        var existing = userAccountRepository.findByEmail(email);
        if (existing.isPresent()) {
            UserAccount u = existing.get();
            if (!"active".equals(u.getStatus())) {
                throw new BusinessException(ResponseCodes.FORBIDDEN, "account_disabled");
            }
            return u;
        }
        if (!settingsService.registrationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "registration_disabled");
        }
        String suffix = randomNumeric(16);
        String seed = "tg_" + verified.providerUserId() + "_" + suffix;
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(seed));
        u.setPasswordSetupRequired(true);
        u.setDisplayName(
                TelegramIdentityHelper.resolveDisplayName(
                        verified.providerUserId(),
                        verified.username(),
                        verified.firstName(),
                        verified.lastName()));
        u.setStatus("active");
        u.setEmailVerified(true);
        memberLevelRepository
                .findByDefaultLevelTrue()
                .ifPresent(ml -> u.setMemberLevelId(ml.getId()));
        final UserAccount saved = userAccountRepository.save(u);
        walletBootstrapService.ensureWallet(saved.getId());
        return saved;
    }

    private static void applyTelegramIdentity(VerifiedTelegramIdentity v, UserOAuthIdentityEntity identity) {
        identity.setUsername(v.username());
        identity.setAvatarUrl(v.avatarUrl());
    }

    private VerifiedTelegramIdentity verifyLoginWidget(TelegramLoginRequest req, TelegramAuthSettings cfg) {
        if (!cfg.enabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "telegram_auth_disabled");
        }
        String botToken = cfg.botToken();
        if (botToken == null || botToken.isBlank()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "telegram_auth_config_invalid");
        }
        String hash = req.hash() == null ? "" : req.hash().trim().toLowerCase(Locale.ROOT);
        if (req.id() <= 0 || req.authDate() <= 0 || hash.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        validateAuthTime(req.authDate(), cfg.loginExpireSeconds());
        String fn = req.firstName() == null ? null : req.firstName().trim();
        String ln = req.lastName() == null ? null : req.lastName().trim();
        String un = req.username() == null ? null : req.username().trim();
        String photo = req.photoUrl() == null ? null : req.photoUrl().trim();
        String dataCheck =
                TelegramWebAuth.buildLoginWidgetDataCheckString(
                        req.id(), req.authDate(), fn, ln, un, photo);
        try {
            String expected = TelegramWebAuth.loginWidgetHash(botToken, dataCheck);
            if (!constantTimeHexEquals(expected, hash)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_signature_invalid");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.INTERNAL, "telegram_auth_config_invalid");
        }
        return new VerifiedTelegramIdentity(
                AuthConstants.OAUTH_PROVIDER_TELEGRAM,
                Long.toString(req.id()),
                un,
                photo,
                fn,
                ln,
                Instant.ofEpochSecond(req.authDate()));
    }

    private VerifiedTelegramIdentity verifyMiniAppInitData(String raw, TelegramAuthSettings cfg) {
        if (!cfg.enabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "telegram_auth_disabled");
        }
        String botToken = cfg.botToken();
        if (botToken == null || botToken.isBlank()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "telegram_auth_config_invalid");
        }
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        Map<String, String> values = parseQuery(trimmed);
        String hash = values.getOrDefault("hash", "").trim().toLowerCase(Locale.ROOT);
        String authDateText = values.getOrDefault("auth_date", "").trim();
        String userRaw = values.getOrDefault("user", "").trim();
        if (hash.isEmpty() || authDateText.isEmpty() || userRaw.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        long authDate;
        try {
            authDate = Long.parseLong(authDateText);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        if (authDate <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        validateAuthTime(authDate, cfg.loginExpireSeconds());
        TelegramMiniAppUserDto user;
        try {
            user = objectMapper.readValue(userRaw, TelegramMiniAppUserDto.class);
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        if (user == null || user.id() <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        String fn = user.firstName() == null ? null : user.firstName().trim();
        String ln = user.lastName() == null ? null : user.lastName().trim();
        String un = user.username() == null ? null : user.username().trim();
        String photo = user.photoUrl() == null ? null : user.photoUrl().trim();
        String dataCheck = TelegramWebAuth.buildMiniAppDataCheckString(values);
        try {
            String expected = TelegramWebAuth.miniAppHash(botToken, dataCheck);
            if (!constantTimeHexEquals(expected, hash)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_signature_invalid");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.INTERNAL, "telegram_auth_config_invalid");
        }
        return new VerifiedTelegramIdentity(
                AuthConstants.OAUTH_PROVIDER_TELEGRAM,
                Long.toString(user.id()),
                un,
                photo,
                fn,
                ln,
                Instant.ofEpochSecond(authDate));
    }

    private static void validateAuthTime(long authDateEpochSeconds, int loginExpireSeconds) {
        Instant authAt = Instant.ofEpochSecond(authDateEpochSeconds);
        Instant now = Instant.now();
        if (authAt.isAfter(now.plus(1, ChronoUnit.MINUTES))) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_payload_invalid");
        }
        long age = now.getEpochSecond() - authAt.getEpochSecond();
        if (age > loginExpireSeconds) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_auth_expired");
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String part : raw.split("&")) {
            int i = part.indexOf('=');
            if (i > 0) {
                String k = URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8);
                m.putIfAbsent(k, v);
            }
        }
        return m;
    }

    private static boolean constantTimeHexEquals(String expectedHex, String actualHex) {
        try {
            byte[] a = HexFormat.of().parseHex(expectedHex.toLowerCase(Locale.ROOT));
            byte[] b = HexFormat.of().parseHex(actualHex.toLowerCase(Locale.ROOT));
            return MessageDigest.isEqual(a, b);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String randomNumeric(int len) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    private record VerifiedTelegramIdentity(
            String provider,
            String providerUserId,
            String username,
            String avatarUrl,
            String firstName,
            String lastName,
            Instant authAt) {}

    private record TelegramMiniAppUserDto(
            long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String username,
            @JsonProperty("photo_url") String photoUrl) {}
}
