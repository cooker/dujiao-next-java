package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserLoginLogEntity;
import com.dujiao.api.dto.admin.AdminLoginLogDto;
import com.dujiao.api.dto.user.LoginLogDto;
import com.dujiao.api.repository.UserLoginLogRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 用户登录日志：与 Go {@code UserLoginLogService} 行为对齐（写入失败不影响登录主流程）。
 */
@Service
public class UserLoginLogService {

    private static final Logger log = LoggerFactory.getLogger(UserLoginLogService.class);

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";

    private final UserLoginLogRepository userLoginLogRepository;
    private final TransactionTemplate requiresNewTx;

    public UserLoginLogService(
            UserLoginLogRepository userLoginLogRepository, PlatformTransactionManager transactionManager) {
        this.userLoginLogRepository = userLoginLogRepository;
        TransactionTemplate t = new TransactionTemplate(transactionManager);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTx = t;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<LoginLogDto>> listForUser(long userId, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        Page<UserLoginLogEntity> result =
                userLoginLogRepository.findByUserIdOrderByIdDesc(userId, pr);
        List<LoginLogDto> list = result.getContent().stream().map(this::toUserDto).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public PageResponse<List<AdminLoginLogDto>> listForAdmin(
            int page,
            int pageSize,
            Long userId,
            String email,
            String status,
            String failReason,
            String clientIp,
            Instant createdFrom,
            Instant createdTo) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        Specification<UserLoginLogEntity> spec = adminSpec(userId, email, status, failReason, clientIp, createdFrom, createdTo);
        Page<UserLoginLogEntity> result = userLoginLogRepository.findAll(spec, pr);
        List<AdminLoginLogDto> list = result.getContent().stream().map(this::toAdminDto).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    private static Specification<UserLoginLogEntity> adminSpec(
            Long userId,
            String email,
            String status,
            String failReason,
            String clientIp,
            Instant createdFrom,
            Instant createdTo) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null && userId > 0) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (email != null && !email.isBlank()) {
                predicates.add(cb.equal(root.get("email"), email.trim().toLowerCase(Locale.ROOT)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (failReason != null && !failReason.isBlank()) {
                predicates.add(cb.equal(root.get("failReason"), failReason.trim()));
            }
            if (clientIp != null && !clientIp.isBlank()) {
                predicates.add(cb.equal(root.get("clientIp"), clientIp.trim()));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** 登录成功：独立事务，失败仅打日志。 */
    public void recordSuccess(long userId, String email, String clientIp, String userAgent, String loginSource) {
        try {
            requiresNewTx.executeWithoutResult(
                    s ->
                            saveRow(
                                    userId,
                                    normalizeEmail(email),
                                    STATUS_SUCCESS,
                                    null,
                                    clientIp,
                                    userAgent,
                                    loginSource,
                                    null));
        } catch (Exception e) {
            log.warn("user_login_log_write_failed status=success userId={}", userId, e);
        }
    }

    /** 登录失败：userId 传 0。 */
    public void recordFailure(
            long userId, String email, String failReason, String clientIp, String userAgent, String loginSource) {
        try {
            requiresNewTx.executeWithoutResult(
                    s ->
                            saveRow(
                                    userId,
                                    normalizeEmail(email),
                                    STATUS_FAILED,
                                    failReason != null && !failReason.isBlank() ? failReason : "internal_error",
                                    clientIp,
                                    userAgent,
                                    loginSource,
                                    null));
        } catch (Exception e) {
            log.warn("user_login_log_write_failed status=failed userId={}", userId, e);
        }
    }

    private void saveRow(
            long userId,
            String email,
            String status,
            String failReason,
            String clientIp,
            String userAgent,
            String loginSource,
            String requestId) {
        UserLoginLogEntity e = new UserLoginLogEntity();
        e.setUserId(userId);
        e.setEmail(email);
        e.setStatus(status);
        e.setFailReason(failReason);
        e.setClientIp(truncate(clientIp, 64));
        e.setUserAgent(userAgent);
        e.setLoginSource(truncate(loginSource, 32));
        e.setRequestId(truncate(requestId, 64));
        userLoginLogRepository.save(e);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private LoginLogDto toUserDto(UserLoginLogEntity e) {
        return new LoginLogDto(
                e.getId(),
                e.getEmail(),
                e.getStatus(),
                e.getClientIp(),
                e.getUserAgent(),
                e.getLoginSource(),
                e.getCreatedAt());
    }

    private AdminLoginLogDto toAdminDto(UserLoginLogEntity e) {
        return new AdminLoginLogDto(
                e.getId(),
                e.getUserId(),
                e.getEmail(),
                e.getStatus(),
                e.getFailReason(),
                e.getClientIp(),
                e.getUserAgent(),
                e.getLoginSource(),
                e.getRequestId(),
                e.getCreatedAt());
    }

    public static Instant parseInstantQuery(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    /** 与 Go 登录失败原因枚举对齐：{@code account_disabled} → {@code user_disabled}。 */
    public static String mapWebLoginFailReason(BusinessException e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) {
            return "internal_error";
        }
        if ("account_disabled".equals(m)) {
            return "user_disabled";
        }
        return m;
    }
}
