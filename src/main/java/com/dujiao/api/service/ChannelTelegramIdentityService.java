package com.dujiao.api.service;

import com.dujiao.api.auth.AuthConstants;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.domain.UserOAuthIdentityEntity;
import com.dujiao.api.dto.channel.ChannelTelegramBindRequest;
import com.dujiao.api.dto.channel.ChannelTelegramIdentityRequest;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.repository.UserOAuthIdentityRepository;
import com.dujiao.api.telegram.TelegramIdentityHelper;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 渠道 Telegram 身份：resolve / provision / bind（与 Go {@code UserAuthService} 渠道逻辑对齐）。
 */
@Service
public class ChannelTelegramIdentityService {

    private final UserOAuthIdentityRepository userOAuthIdentityRepository;
    private final UserAccountRepository userAccountRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final EmailVerificationService emailVerificationService;
    private final WalletBootstrapService walletBootstrapService;

    public ChannelTelegramIdentityService(
            UserOAuthIdentityRepository userOAuthIdentityRepository,
            UserAccountRepository userAccountRepository,
            MemberLevelRepository memberLevelRepository,
            PasswordEncoder passwordEncoder,
            SettingsService settingsService,
            EmailVerificationService emailVerificationService,
            WalletBootstrapService walletBootstrapService) {
        this.userOAuthIdentityRepository = userOAuthIdentityRepository;
        this.userAccountRepository = userAccountRepository;
        this.memberLevelRepository = memberLevelRepository;
        this.passwordEncoder = passwordEncoder;
        this.settingsService = settingsService;
        this.emailVerificationService = emailVerificationService;
        this.walletBootstrapService = walletBootstrapService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resolve(ChannelTelegramIdentityRequest req) {
        VerifiedChannelIdentity v = verifiedFrom(req);
        var pair = resolveInternal(v);
        if (pair.identity == null) {
            return Map.of("bound", false);
        }
        return buildIdentityResponse(true, false, pair.user, pair.identity);
    }

    @Transactional
    public Map<String, Object> provision(ChannelTelegramIdentityRequest req) {
        VerifiedChannelIdentity v = verifiedFrom(req);
        IdentityPair pair = resolveInternal(v);
        if (pair.identity != null) {
            return buildIdentityResponse(true, false, pair.user, pair.identity);
        }

        String phEmail = TelegramIdentityHelper.placeholderEmail(v.providerUserId());
        boolean createdUser = userAccountRepository.findByEmail(phEmail).isEmpty();

        UserAccount user = findOrCreateTelegramUser(v);

        Optional<UserOAuthIdentityEntity> byUser =
                userOAuthIdentityRepository.findByUserIdAndProvider(
                        user.getId(), AuthConstants.OAUTH_PROVIDER_TELEGRAM);
        if (byUser.isPresent()) {
            UserOAuthIdentityEntity e = byUser.get();
            if (!v.providerUserId().equals(e.getProviderUserId())) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_already_bound");
            }
            if (applyTelegramFields(v, e)) {
                userOAuthIdentityRepository.save(e);
            }
            return buildIdentityResponse(true, createdUser, user, e);
        }

        UserOAuthIdentityEntity identity = new UserOAuthIdentityEntity();
        identity.setUserId(user.getId());
        identity.setProvider(AuthConstants.OAUTH_PROVIDER_TELEGRAM);
        identity.setProviderUserId(v.providerUserId());
        identity.setUsername(v.username());
        identity.setAvatarUrl(v.avatarUrl());
        try {
            userOAuthIdentityRepository.save(identity);
        } catch (DataIntegrityViolationException e) {
            var race =
                    userOAuthIdentityRepository
                            .findByProviderAndProviderUserId(
                                    AuthConstants.OAUTH_PROVIDER_TELEGRAM, v.providerUserId())
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ResponseCodes.BAD_REQUEST, "telegram_identity_conflict"));
            UserAccount u =
                    userAccountRepository
                            .findById(race.getUserId())
                            .orElseThrow(
                                    () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
            return buildIdentityResponse(true, false, u, race);
        }
        return buildIdentityResponse(true, createdUser, user, identity);
    }

    @Transactional
    public Map<String, Object> bindByEmailCode(ChannelTelegramBindRequest req) {
        VerifiedChannelIdentity v = verifiedFromBind(req);
        String email = normalizeEmail(req.email());
        if (email.isEmpty() || req.code() == null || req.code().isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        emailVerificationService.verifyAndConsumeCode(
                email, AuthConstants.PURPOSE_TELEGRAM_BIND, req.code());

        UserAccount target =
                userAccountRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!"active".equalsIgnoreCase(target.getStatus())) {
            throw new BusinessException(ResponseCodes.UNAUTHORIZED, "user_disabled");
        }

        long previousUserId = bindTelegramIdentityToUser(target, v);
        UserOAuthIdentityEntity identity =
                userOAuthIdentityRepository
                        .findByProviderAndProviderUserId(
                                AuthConstants.OAUTH_PROVIDER_TELEGRAM, v.providerUserId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.INTERNAL, "internal_error"));
        Map<String, Object> resp = buildIdentityResponse(true, false, target, identity);
        if (previousUserId > 0) {
            resp.put("previous_user_id", previousUserId);
        }
        return resp;
    }

    /**
     * 与 Go {@code provisionTelegramChannelUserID} 一致：按渠道用户标识解析用户，不存在时预置占位用户并返回 userId。
     */
    @Transactional
    public long provisionAndGetUserId(String channelUserId, String telegramUserId) {
        String raw = firstNonBlank(channelUserId, telegramUserId);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "channel_user_id_required");
        }
        VerifiedChannelIdentity v = new VerifiedChannelIdentity(raw.trim(), "", "", "", "");
        IdentityPair pair = resolveInternal(v);
        if (pair.user != null) {
            return pair.user.getId();
        }
        UserAccount user = findOrCreateTelegramUser(v);
        Optional<UserOAuthIdentityEntity> existing =
                userOAuthIdentityRepository.findByProviderAndProviderUserId(
                        AuthConstants.OAUTH_PROVIDER_TELEGRAM, v.providerUserId());
        if (existing.isPresent()) {
            return existing.get().getUserId();
        }
        UserOAuthIdentityEntity identity = new UserOAuthIdentityEntity();
        identity.setUserId(user.getId());
        identity.setProvider(AuthConstants.OAUTH_PROVIDER_TELEGRAM);
        identity.setProviderUserId(v.providerUserId());
        try {
            userOAuthIdentityRepository.save(identity);
        } catch (DataIntegrityViolationException e) {
            return userOAuthIdentityRepository
                    .findByProviderAndProviderUserId(AuthConstants.OAUTH_PROVIDER_TELEGRAM, v.providerUserId())
                    .map(UserOAuthIdentityEntity::getUserId)
                    .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_identity_conflict"));
        }
        return user.getId();
    }

    private long bindTelegramIdentityToUser(UserAccount targetUser, VerifiedChannelIdentity v) {
        Optional<UserOAuthIdentityEntity> currentOpt =
                userOAuthIdentityRepository.findByUserIdAndProvider(
                        targetUser.getId(), AuthConstants.OAUTH_PROVIDER_TELEGRAM);
        if (currentOpt.isPresent()
                && !v.providerUserId().equals(currentOpt.get().getProviderUserId())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_already_bound");
        }

        Optional<UserOAuthIdentityEntity> occupiedOpt =
                userOAuthIdentityRepository.findByProviderAndProviderUserId(
                        AuthConstants.OAUTH_PROVIDER_TELEGRAM, v.providerUserId());

        if (occupiedOpt.isEmpty()) {
            UserOAuthIdentityEntity identity = new UserOAuthIdentityEntity();
            identity.setUserId(targetUser.getId());
            identity.setProvider(AuthConstants.OAUTH_PROVIDER_TELEGRAM);
            identity.setProviderUserId(v.providerUserId());
            identity.setUsername(v.username());
            identity.setAvatarUrl(v.avatarUrl());
            userOAuthIdentityRepository.save(identity);
            return 0L;
        }

        UserOAuthIdentityEntity occupied = occupiedOpt.get();
        if (occupied.getUserId().equals(targetUser.getId())) {
            if (applyTelegramFields(v, occupied)) {
                userOAuthIdentityRepository.save(occupied);
            }
            return 0L;
        }

        UserAccount previous =
                userAccountRepository
                        .findById(occupied.getUserId())
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!TelegramIdentityHelper.isPlaceholderEmail(previous.getEmail())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "telegram_bind_conflict");
        }

        long previousUserId = occupied.getUserId();
        occupied.setUserId(targetUser.getId());
        applyTelegramFields(v, occupied);
        userOAuthIdentityRepository.save(occupied);
        return previousUserId;
    }

    private IdentityPair resolveInternal(VerifiedChannelIdentity v) {
        Optional<UserOAuthIdentityEntity> idOpt =
                userOAuthIdentityRepository.findByProviderAndProviderUserId(
                        AuthConstants.OAUTH_PROVIDER_TELEGRAM, v.providerUserId());
        if (idOpt.isEmpty()) {
            return new IdentityPair(null, null);
        }
        UserOAuthIdentityEntity identity = idOpt.get();
        UserAccount user =
                userAccountRepository
                        .findById(identity.getUserId())
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResponseCodes.UNAUTHORIZED, "user_disabled");
        }
        if (applyTelegramFields(v, identity)) {
            userOAuthIdentityRepository.save(identity);
        }
        return new IdentityPair(user, identity);
    }

    private UserAccount findOrCreateTelegramUser(VerifiedChannelIdentity v) {
        String email = TelegramIdentityHelper.placeholderEmail(v.providerUserId());
        Optional<UserAccount> existing = userAccountRepository.findByEmail(email);
        if (existing.isPresent()) {
            UserAccount u = existing.get();
            if (!"active".equalsIgnoreCase(u.getStatus())) {
                throw new BusinessException(ResponseCodes.UNAUTHORIZED, "user_disabled");
            }
            return u;
        }
        if (!settingsService.registrationEnabled()) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "registration_disabled");
        }
        String suffix = randomNumeric(16);
        String seed = "tg_" + v.providerUserId() + "_" + suffix;
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(seed));
        u.setPasswordSetupRequired(true);
        u.setDisplayName(
                TelegramIdentityHelper.resolveDisplayName(
                        v.providerUserId(), v.username(), v.firstName(), v.lastName()));
        u.setStatus("active");
        u.setEmailVerified(true);
        memberLevelRepository.findByDefaultLevelTrue().ifPresent(ml -> u.setMemberLevelId(ml.getId()));
        UserAccount saved = userAccountRepository.save(u);
        walletBootstrapService.ensureWallet(saved.getId());
        return saved;
    }

    private static boolean applyTelegramFields(
            VerifiedChannelIdentity v, UserOAuthIdentityEntity identity) {
        boolean changed = false;
        if (v.username() != null
                && !v.username().isBlank()
                && !v.username().equals(identity.getUsername())) {
            identity.setUsername(v.username());
            changed = true;
        }
        if (v.avatarUrl() != null
                && !v.avatarUrl().isBlank()
                && !v.avatarUrl().equals(identity.getAvatarUrl())) {
            identity.setAvatarUrl(v.avatarUrl());
            changed = true;
        }
        return changed;
    }

    private static Map<String, Object> buildIdentityResponse(
            boolean bound, boolean created, UserAccount user, UserOAuthIdentityEntity identity) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("bound", bound);
        if (identity != null) {
            Map<String, Object> idMap = new LinkedHashMap<>();
            idMap.put("provider", identity.getProvider());
            idMap.put("provider_user_id", identity.getProviderUserId());
            idMap.put("username", identity.getUsername() != null ? identity.getUsername() : "");
            idMap.put("avatar_url", identity.getAvatarUrl() != null ? identity.getAvatarUrl() : "");
            resp.put("identity", idMap);
        }
        if (user != null) {
            Map<String, Object> uMap = new LinkedHashMap<>();
            uMap.put("id", user.getId());
            uMap.put("email", user.getEmail());
            uMap.put("display_name", user.getDisplayName() != null ? user.getDisplayName() : "");
            uMap.put("status", user.getStatus());
            uMap.put("locale", "");
            uMap.put("email_verified", user.isEmailVerified());
            uMap.put("password_setup_required", user.isPasswordSetupRequired());
            resp.put("user", uMap);
        }
        if (bound) {
            resp.put("created", created);
        }
        return resp;
    }

    private static VerifiedChannelIdentity verifiedFrom(ChannelTelegramIdentityRequest req) {
        String channelUserId = firstNonBlank(req.channelUserId(), req.telegramUserId());
        if (channelUserId == null || channelUserId.isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        String username = firstNonBlank(req.username(), req.telegramUsername());
        return new VerifiedChannelIdentity(
                channelUserId.trim(),
                username == null ? "" : username.trim(),
                req.avatarUrl() == null ? "" : req.avatarUrl().trim(),
                req.firstName() == null ? "" : req.firstName().trim(),
                req.lastName() == null ? "" : req.lastName().trim());
    }

    private static VerifiedChannelIdentity verifiedFromBind(ChannelTelegramBindRequest req) {
        String channelUserId = firstNonBlank(req.channelUserId(), req.telegramUserId());
        if (channelUserId == null || channelUserId.isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        String username = firstNonBlank(req.username(), req.telegramUsername());
        return new VerifiedChannelIdentity(
                channelUserId.trim(),
                username == null ? "" : username.trim(),
                req.avatarUrl() == null ? "" : req.avatarUrl().trim(),
                req.firstName() == null ? "" : req.firstName().trim(),
                req.lastName() == null ? "" : req.lastName().trim());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return "";
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.toLowerCase(Locale.ROOT).trim();
    }

    private static String randomNumeric(int len) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    private record VerifiedChannelIdentity(
            String providerUserId,
            String username,
            String avatarUrl,
            String firstName,
            String lastName) {}

    private record IdentityPair(UserAccount user, UserOAuthIdentityEntity identity) {}
}
