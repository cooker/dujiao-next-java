package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserApiCredentialEntity;
import com.dujiao.api.repository.UserApiCredentialRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户侧 API 凭证，与 Go {@code ApiCredentialService}（用户接口部分）行为对齐。 状态：{@code pending}、{@code
 * active}（审核通过后，对外 JSON 中映射为 {@code approved}）、{@code rejected}。
 */
@Service
public class UserApiCredentialService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserApiCredentialRepository userApiCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApiCredentialService(
            UserApiCredentialRepository userApiCredentialRepository, PasswordEncoder passwordEncoder) {
        this.userApiCredentialRepository = userApiCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForUser(long userId) {
        Optional<UserApiCredentialEntity> opt = userApiCredentialRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return Map.of("status", "none");
        }
        UserApiCredentialEntity c = opt.get();
        String ps = publicStatus(c.getStatus());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("status", ps);
        m.put("is_active", c.isActive());
        if (c.getCreatedAt() != null) {
            m.put("created_at", c.getCreatedAt());
        }
        if ("rejected".equals(c.getStatus()) && c.getRejectReason() != null && !c.getRejectReason().isBlank()) {
            m.put("reject_reason", c.getRejectReason());
        }
        if ("active".equals(c.getStatus())) {
            m.put("api_key", c.getApiKey());
            if (c.getApprovedAt() != null) {
                m.put("approved_at", c.getApprovedAt());
            }
            if (c.getLastUsedAt() != null) {
                m.put("last_used_at", c.getLastUsedAt());
            }
            if (c.getSecretSuffix() != null && !c.getSecretSuffix().isBlank()) {
                m.put("api_secret_tail", c.getSecretSuffix());
            }
        }
        return m;
    }

    @Transactional
    public Map<String, Object> apply(long userId) {
        Optional<UserApiCredentialEntity> opt = userApiCredentialRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            UserApiCredentialEntity e = new UserApiCredentialEntity();
            e.setUserId(userId);
            e.setApiKey(randomHex(32));
            e.setStatus("pending");
            e.setSecretHash(null);
            e.setActive(false);
            userApiCredentialRepository.save(e);
            return Map.of("id", e.getId(), "status", "pending_review");
        }
        UserApiCredentialEntity e = opt.get();
        if ("pending".equals(e.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "api_credential_pending_exist");
        }
        if ("active".equals(e.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "api_credential_exists");
        }
        if ("rejected".equals(e.getStatus())) {
            e.setApiKey(randomHex(32));
            e.setStatus("pending");
            e.setSecretHash(null);
            e.setRejectReason(null);
            e.setApprovedAt(null);
            e.setLastUsedAt(null);
            e.setSecretSuffix(null);
            e.setActive(false);
            userApiCredentialRepository.save(e);
            return Map.of("id", e.getId(), "status", "pending_review");
        }
        throw new BusinessException(ResponseCodes.BAD_REQUEST, "api_credential_exists");
    }

    @Transactional
    public Map<String, Object> regenerateSecret(long userId) {
        UserApiCredentialEntity c =
                userApiCredentialRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "api_credential_not_found"));
        if (!"active".equals(c.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "api_credential_not_approved");
        }
        String plain = randomHex(64);
        c.setSecretHash(passwordEncoder.encode(plain));
        c.setSecretSuffix(secretTail(plain));
        userApiCredentialRepository.save(c);
        return Map.of("api_secret", plain);
    }

    @Transactional
    public void setActive(long userId, boolean active) {
        UserApiCredentialEntity c =
                userApiCredentialRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "api_credential_not_found"));
        if (!"active".equals(c.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "api_credential_not_approved");
        }
        c.setActive(active);
        userApiCredentialRepository.save(c);
    }

    /** 与 Go 前台展示一致：{@code active} → {@code approved}，{@code pending} → {@code pending_review}。 */
    private static String publicStatus(String stored) {
        if ("active".equals(stored)) {
            return "approved";
        }
        if ("pending".equals(stored)) {
            return "pending_review";
        }
        return stored;
    }

    private static String secretTail(String plain) {
        if (plain == null || plain.length() < 4) {
            return "";
        }
        return plain.substring(plain.length() - 4);
    }

    private static String randomHex(int numBytes) {
        byte[] b = new byte[numBytes];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
