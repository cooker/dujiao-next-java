package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.AdminAccount;
import com.dujiao.api.domain.AuthzAuditLogEntity;
import com.dujiao.api.domain.AuthzRuleEntity;
import com.dujiao.api.repository.AdminAccountRepository;
import com.dujiao.api.repository.AuthzAuditLogRepository;
import com.dujiao.api.repository.AuthzRuleRepository;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.web.ApiPaths;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/authz")
@Transactional
public class AdminAuthzController {
    private static final String PTYPE_POLICY = "p";
    private static final String PTYPE_GROUP = "g";
    private static final String ROLE_PREFIX = "role:";
    private static final String ROLE_ANCHOR = "role:__anchor__";
    private static final String PROTECTED_SUPER_ADMIN_USERNAME = "admin";
    private static final List<String> BUILTIN_ROLES =
            List.of(
                    "readonly_auditor",
                    "operations",
                    "support",
                    "integration",
                    "finance",
                    "system_admin");
    private static final Map<String, List<String>> BUILTIN_ROLE_INHERITS =
            Map.of(
                    "operations", List.of("readonly_auditor"),
                    "support", List.of("readonly_auditor"),
                    "integration", List.of("readonly_auditor"),
                    "finance", List.of("readonly_auditor"),
                    "system_admin", List.of("readonly_auditor"));

    private final AdminAccountRepository adminAccountRepository;
    private final AuthzRuleRepository authzRuleRepository;
    private final AuthzAuditLogRepository authzAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public AdminAuthzController(
            AdminAccountRepository adminAccountRepository,
            AuthzRuleRepository authzRuleRepository,
            AuthzAuditLogRepository authzAuditLogRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.adminAccountRepository = adminAccountRepository;
        this.authzRuleRepository = authzRuleRepository;
        this.authzAuditLogRepository = authzAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @PostConstruct
    public void bootstrapBuiltinRoles() {
        seedBuiltinRolePolicies();
        for (String role : BUILTIN_ROLES) {
            ensureRole(normalizeRole(role));
        }
    }

    private void seedBuiltinRolePolicies() {
        // readonly_auditor
        seedPolicies(
                "readonly_auditor",
                List.of(
                        new PolicySeed("/admin/*", "GET"),
                        new PolicySeed("/admin/password", "PUT"),
                        new PolicySeed("/admin/ads/impression", "POST")));

        // operations
        seedPolicies(
                "operations",
                List.of(
                        new PolicySeed("/admin/products", "*"),
                        new PolicySeed("/admin/products/:id", "*"),
                        new PolicySeed("/admin/categories", "*"),
                        new PolicySeed("/admin/categories/:id", "*"),
                        new PolicySeed("/admin/posts", "*"),
                        new PolicySeed("/admin/posts/:id", "*"),
                        new PolicySeed("/admin/banners", "*"),
                        new PolicySeed("/admin/banners/:id", "*"),
                        new PolicySeed("/admin/coupons", "*"),
                        new PolicySeed("/admin/coupons/:id", "*"),
                        new PolicySeed("/admin/promotions", "*"),
                        new PolicySeed("/admin/promotions/:id", "*"),
                        new PolicySeed("/admin/card-secrets", "*"),
                        new PolicySeed("/admin/card-secrets/:id", "*"),
                        new PolicySeed("/admin/card-secrets/batch", "POST"),
                        new PolicySeed("/admin/card-secrets/import", "POST"),
                        new PolicySeed("/admin/card-secrets/batch-status", "PATCH"),
                        new PolicySeed("/admin/card-secrets/batch-delete", "POST"),
                        new PolicySeed("/admin/card-secrets/export", "POST"),
                        new PolicySeed("/admin/card-secrets/stats", "GET"),
                        new PolicySeed("/admin/card-secrets/batches", "GET"),
                        new PolicySeed("/admin/card-secrets/template", "GET"),
                        new PolicySeed("/admin/gift-cards", "*"),
                        new PolicySeed("/admin/gift-cards/:id", "*"),
                        new PolicySeed("/admin/gift-cards/generate", "POST"),
                        new PolicySeed("/admin/gift-cards/batch-status", "PATCH"),
                        new PolicySeed("/admin/gift-cards/export", "POST"),
                        new PolicySeed("/admin/upload", "POST"),
                        new PolicySeed("/admin/affiliates/users", "GET"),
                        new PolicySeed("/admin/affiliates/users/:id/status", "PATCH"),
                        new PolicySeed("/admin/affiliates/users/batch-status", "PATCH"),
                        new PolicySeed("/admin/member-levels", "*"),
                        new PolicySeed("/admin/member-levels/:id", "*"),
                        new PolicySeed("/admin/member-levels/backfill", "POST"),
                        new PolicySeed("/admin/member-level-prices", "*"),
                        new PolicySeed("/admin/member-level-prices/batch", "POST"),
                        new PolicySeed("/admin/member-level-prices/:id", "DELETE")));

        // support
        seedPolicies(
                "support",
                List.of(
                        new PolicySeed("/admin/orders", "GET"),
                        new PolicySeed("/admin/orders/:id", "GET"),
                        new PolicySeed("/admin/orders/:id", "PATCH"),
                        new PolicySeed("/admin/orders/:id/fulfillment/download", "GET"),
                        new PolicySeed("/admin/orders/:id/refund-to-wallet", "POST"),
                        new PolicySeed("/admin/fulfillments", "POST"),
                        new PolicySeed("/admin/users", "GET"),
                        new PolicySeed("/admin/users/:id", "GET"),
                        new PolicySeed("/admin/users/:id", "PUT"),
                        new PolicySeed("/admin/users/batch-status", "PUT"),
                        new PolicySeed("/admin/users/:id/coupon-usages", "GET"),
                        new PolicySeed("/admin/users/:id/wallet", "GET"),
                        new PolicySeed("/admin/users/:id/wallet/transactions", "GET"),
                        new PolicySeed("/admin/users/:id/wallet/adjust", "POST"),
                        new PolicySeed("/admin/users/:id/member-level", "PUT"),
                        new PolicySeed("/admin/user-login-logs", "GET"),
                        new PolicySeed("/admin/wallet/recharges", "GET"),
                        new PolicySeed("/admin/payments", "GET"),
                        new PolicySeed("/admin/payments/:id", "GET"),
                        new PolicySeed("/admin/gift-cards", "GET")));

        // integration
        seedPolicies(
                "integration",
                List.of(
                        new PolicySeed("/admin/site-connections", "*"),
                        new PolicySeed("/admin/site-connections/:id", "*"),
                        new PolicySeed("/admin/site-connections/:id/ping", "POST"),
                        new PolicySeed("/admin/site-connections/:id/status", "PUT"),
                        new PolicySeed("/admin/site-connections/:id/reapply-markup", "POST"),
                        new PolicySeed("/admin/product-mappings", "*"),
                        new PolicySeed("/admin/product-mappings/:id", "*"),
                        new PolicySeed("/admin/product-mappings/:id/sync", "POST"),
                        new PolicySeed("/admin/product-mappings/:id/status", "PUT"),
                        new PolicySeed("/admin/product-mappings/import", "POST"),
                        new PolicySeed("/admin/product-mappings/batch-import", "POST"),
                        new PolicySeed("/admin/product-mappings/batch-sync", "POST"),
                        new PolicySeed("/admin/product-mappings/batch-status", "POST"),
                        new PolicySeed("/admin/product-mappings/batch-delete", "POST"),
                        new PolicySeed("/admin/procurement-orders", "GET"),
                        new PolicySeed("/admin/procurement-orders/:id", "GET"),
                        new PolicySeed("/admin/procurement-orders/:id/upstream-payload/download", "GET"),
                        new PolicySeed("/admin/procurement-orders/:id/retry", "POST"),
                        new PolicySeed("/admin/procurement-orders/:id/cancel", "POST"),
                        new PolicySeed("/admin/reconciliation/run", "POST"),
                        new PolicySeed("/admin/reconciliation/jobs", "GET"),
                        new PolicySeed("/admin/reconciliation/jobs/:id", "GET"),
                        new PolicySeed("/admin/reconciliation/items/:id/resolve", "PUT"),
                        new PolicySeed("/admin/api-credentials", "*"),
                        new PolicySeed("/admin/api-credentials/:id", "*"),
                        new PolicySeed("/admin/api-credentials/:id/approve", "POST"),
                        new PolicySeed("/admin/api-credentials/:id/reject", "POST"),
                        new PolicySeed("/admin/api-credentials/:id/status", "PUT"),
                        new PolicySeed("/admin/upstream-products", "GET")));

        // finance
        seedPolicies(
                "finance",
                List.of(
                        new PolicySeed("/admin/payments", "GET"),
                        new PolicySeed("/admin/payments/:id", "GET"),
                        new PolicySeed("/admin/payments/export", "GET"),
                        new PolicySeed("/admin/payment-channels", "*"),
                        new PolicySeed("/admin/payment-channels/:id", "*"),
                        new PolicySeed("/admin/orders", "GET"),
                        new PolicySeed("/admin/orders/:id", "GET"),
                        new PolicySeed("/admin/orders/:id/refund-to-wallet", "POST"),
                        new PolicySeed("/admin/affiliates/commissions", "GET"),
                        new PolicySeed("/admin/affiliates/withdraws", "GET"),
                        new PolicySeed("/admin/affiliates/withdraws/:id/reject", "POST"),
                        new PolicySeed("/admin/affiliates/withdraws/:id/pay", "POST"),
                        new PolicySeed("/admin/gift-cards", "GET"),
                        new PolicySeed("/admin/gift-cards/export", "POST"),
                        new PolicySeed("/admin/wallet/recharges", "GET")));

        // system_admin
        seedPolicies(
                "system_admin",
                List.of(
                        new PolicySeed("/admin/settings", "*"),
                        new PolicySeed("/admin/settings/smtp", "*"),
                        new PolicySeed("/admin/settings/smtp/test", "POST"),
                        new PolicySeed("/admin/settings/captcha", "*"),
                        new PolicySeed("/admin/settings/telegram-auth", "*"),
                        new PolicySeed("/admin/settings/notification-center", "*"),
                        new PolicySeed("/admin/settings/notification-center/logs", "GET"),
                        new PolicySeed("/admin/settings/notification-center/test", "POST"),
                        new PolicySeed("/admin/settings/notifications", "*"),
                        new PolicySeed("/admin/settings/notifications/logs", "GET"),
                        new PolicySeed("/admin/settings/notifications/test", "POST"),
                        new PolicySeed("/admin/settings/order-email-template", "*"),
                        new PolicySeed("/admin/settings/order-email-template/reset", "POST"),
                        new PolicySeed("/admin/settings/affiliate", "*"),
                        new PolicySeed("/admin/settings/telegram-bot", "*"),
                        new PolicySeed("/admin/settings/telegram-bot/runtime-status", "GET"),
                        new PolicySeed("/admin/authz/me", "GET"),
                        new PolicySeed("/admin/authz/roles", "*"),
                        new PolicySeed("/admin/authz/roles/:role", "*"),
                        new PolicySeed("/admin/authz/roles/:role/policies", "GET"),
                        new PolicySeed("/admin/authz/admins", "*"),
                        new PolicySeed("/admin/authz/admins/:id", "*"),
                        new PolicySeed("/admin/authz/admins/:id/roles", "*"),
                        new PolicySeed("/admin/authz/policies", "*"),
                        new PolicySeed("/admin/authz/permissions/catalog", "GET"),
                        new PolicySeed("/admin/authz/audit-logs", "GET"),
                        new PolicySeed("/admin/channel-clients", "*"),
                        new PolicySeed("/admin/channel-clients/:id", "*"),
                        new PolicySeed("/admin/channel-clients/:id/status", "PUT"),
                        new PolicySeed("/admin/channel-clients/:id/reset-secret", "POST"),
                        new PolicySeed("/admin/telegram-bot/broadcasts", "*"),
                        new PolicySeed("/admin/telegram-bot/users", "GET")));
    }

    private void seedPolicies(String roleRaw, List<PolicySeed> policies) {
        String role = ensureRole(normalizeRole(roleRaw));
        for (String parent : BUILTIN_ROLE_INHERITS.getOrDefault(roleRaw, List.of())) {
            ensureGrouping(role, normalizeRole(parent));
        }
        for (PolicySeed p : policies) {
            ensurePolicy(role, normalizeObject(p.object()), normalizeAction(p.action()));
        }
    }

    private void ensureGrouping(String v0, String v1) {
        if (authzRuleRepository.findByPtypeAndV0AndV1(PTYPE_GROUP, v0, v1).isEmpty()) {
            AuthzRuleEntity e = new AuthzRuleEntity();
            e.setPtype(PTYPE_GROUP);
            e.setV0(v0);
            e.setV1(v1);
            e.setV2(null);
            authzRuleRepository.save(e);
        }
    }

    private void ensurePolicy(String role, String object, String action) {
        if (authzRuleRepository.findByPtypeAndV0AndV1AndV2(PTYPE_POLICY, role, object, action).isEmpty()) {
            AuthzRuleEntity e = new AuthzRuleEntity();
            e.setPtype(PTYPE_POLICY);
            e.setV0(role);
            e.setV1(object);
            e.setV2(action);
            authzRuleRepository.save(e);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
        long adminId = SecurityUtils.requireAdminId();
        AdminAccount admin =
                adminAccountRepository
                        .findById(adminId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "admin_not_found"));

        List<String> roles = getAdminRolesInternal(admin.getId());
        List<Map<String, String>> policies = getAdminPoliciesInternal(admin.getId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        new LinkedHashMap<>(
                                Map.of(
                                        "admin_id", admin.getId(),
                                        "is_super", admin.isSuperAdmin(),
                                        "roles", roles,
                                        "policies", policies))));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<String>>> listRoles() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(listRolesInternal()));
    }

    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAdmins() {
        SecurityUtils.requireAdminId();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AdminAccount admin : adminAccountRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", admin.getId());
            row.put("username", admin.getUsername());
            row.put("is_super", admin.isSuperAdmin());
            row.put("last_login_at", null);
            row.put("created_at", null);
            row.put("roles", getAdminRolesInternal(admin.getId()));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(x -> ((Number) x.get("id")).longValue()));
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<List<Map<String, Object>>>> auditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "operator_admin_id", required = false) Long operatorAdminId,
            @RequestParam(name = "target_admin_id", required = false) Long targetAdminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String object,
            @RequestParam(required = false) String method,
            @RequestParam(name = "created_from", required = false) String createdFrom,
            @RequestParam(name = "created_to", required = false) String createdTo) {
        SecurityUtils.requireAdminId();
        Specification<AuthzAuditLogEntity> spec =
                (root, query, cb) -> {
                    List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
                    if (operatorAdminId != null) {
                        ps.add(cb.equal(root.get("operatorAdminId"), operatorAdminId));
                    }
                    if (targetAdminId != null) {
                        ps.add(cb.equal(root.get("targetAdminId"), targetAdminId));
                    }
                    if (notBlank(action)) {
                        ps.add(cb.equal(root.get("action"), action.trim()));
                    }
                    if (notBlank(role)) {
                        ps.add(cb.equal(root.get("role"), role.trim()));
                    }
                    if (notBlank(object)) {
                        ps.add(cb.equal(root.get("object"), normalizeObject(object)));
                    }
                    if (notBlank(method)) {
                        ps.add(cb.equal(root.get("method"), normalizeAction(method)));
                    }
                    Instant from = parseInstantNullable(createdFrom);
                    Instant to = parseInstantNullable(createdTo);
                    if (from != null) {
                        ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                    }
                    if (to != null) {
                        ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                    }
                    return cb.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        Page<AuthzAuditLogEntity> rows =
                authzAuditLogRepository.findAll(
                        spec,
                        PageRequest.of(p - 1, ps, org.springframework.data.domain.Sort.by("id").descending()));
        List<Map<String, Object>> data =
                rows.getContent().stream()
                        .map(
                                x -> {
                                    Map<String, Object> row = new LinkedHashMap<>();
                                    row.put("id", x.getId());
                                    row.put("operator_admin_id", x.getOperatorAdminId());
                                    row.put("operator_username", x.getOperatorUsername());
                                    row.put("target_admin_id", x.getTargetAdminId());
                                    row.put("target_username", x.getTargetUsername());
                                    row.put("action", x.getAction());
                                    row.put("role", x.getRole());
                                    row.put("object", x.getObject());
                                    row.put("method", x.getMethod());
                                    row.put("request_id", x.getRequestId());
                                    row.put("detail", parseJsonOrNull(x.getDetailJson()));
                                    row.put("created_at", x.getCreatedAt());
                                    return row;
                                })
                        .toList();
        return ResponseEntity.ok(
                PageResponse.success(data, PaginationDto.of(rows.getNumber() + 1, rows.getSize(), rows.getTotalElements())));
    }

    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAdmin(
            @Valid @RequestBody CreateAdminRequest req) {
        long operatorId = SecurityUtils.requireAdminId();
        String username = normalizeUsername(req.username());
        if (adminAccountRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "admin_username_exists");
        }
        AdminAccount admin = new AdminAccount();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(req.password().trim()));
        boolean isSuper = req.isSuper() != null && req.isSuper();
        if (PROTECTED_SUPER_ADMIN_USERNAME.equalsIgnoreCase(username)) {
            isSuper = true;
        }
        admin.setSuperAdmin(isSuper);
        AdminAccount saved = adminAccountRepository.save(admin);
        if (saved.isSuperAdmin()) {
            setAdminRolesInternal(saved.getId(), List.of("role:system_admin"));
        } else {
            setAdminRolesInternal(saved.getId(), List.of("role:readonly_auditor"));
        }
        recordAudit(operatorId, "admin_create", null, null, null, Map.of("target_admin_id", saved.getId()));
        return ResponseEntity.ok(ApiResponse.success(adminRow(saved)));
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAdmin(
            @PathVariable String id, @RequestBody UpdateAdminRequest req) {
        long operatorId = SecurityUtils.requireAdminId();
        long adminId = parsePositiveLong(id, "admin_id_invalid");
        AdminAccount admin =
                adminAccountRepository
                        .findById(adminId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "admin_id_invalid"));
        if (req.username() != null) {
            String username = normalizeUsername(req.username());
            var existing = adminAccountRepository.findByUsername(username).orElse(null);
            if (existing != null && !existing.getId().equals(admin.getId())) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "admin_username_exists");
            }
            admin.setUsername(username);
        }
        if (req.password() != null && !req.password().trim().isEmpty()) {
            admin.setPasswordHash(passwordEncoder.encode(req.password().trim()));
        }
        if (req.isSuper() != null) {
            boolean isSuper = req.isSuper();
            if (PROTECTED_SUPER_ADMIN_USERNAME.equalsIgnoreCase(admin.getUsername())) {
                isSuper = true;
            }
            admin.setSuperAdmin(isSuper);
        }
        AdminAccount saved = adminAccountRepository.save(admin);
        recordAudit(operatorId, "admin_update", null, null, null, Map.of("target_admin_id", saved.getId()));
        return ResponseEntity.ok(ApiResponse.success(adminRow(saved)));
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable String id) {
        long operatorId = SecurityUtils.requireAdminId();
        long adminId = parsePositiveLong(id, "admin_id_invalid");
        if (operatorId == adminId) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "admin_delete_self_forbidden");
        }
        AdminAccount admin =
                adminAccountRepository
                        .findById(adminId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "admin_id_invalid"));
        if (PROTECTED_SUPER_ADMIN_USERNAME.equalsIgnoreCase(admin.getUsername())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "admin_delete_protected");
        }
        if (adminAccountRepository.count() <= 1) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "admin_delete_last_forbidden");
        }
        authzRuleRepository.deleteByPtypeAndV0(PTYPE_GROUP, subjectForAdmin(adminId));
        adminAccountRepository.delete(admin);
        recordAudit(operatorId, "admin_delete", null, null, null, Map.of("target_admin_id", adminId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/permissions/catalog")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> permissionCatalog() {
        SecurityUtils.requireAdminId();
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        requestMappingHandlerMapping
                .getHandlerMethods()
                .forEach(
                        (info, handlerMethod) -> {
                            Set<String> paths = info.getPatternValues();
                            Set<org.springframework.web.bind.annotation.RequestMethod> methods =
                                    info.getMethodsCondition().getMethods();
                            for (String path : paths) {
                                if (!path.startsWith("/api/v1/admin/") || "/api/v1/admin/login".equals(path)) {
                                    continue;
                                }
                                for (var m : methods) {
                                    String method = m.name();
                                    String object = normalizeObject(path);
                                    String permission = method + ":" + object;
                                    if (!seen.add(permission)) {
                                        continue;
                                    }
                                    items.add(
                                            Map.of(
                                                    "module", deriveModule(object),
                                                    "method", method,
                                                    "object", object,
                                                    "permission", permission));
                                }
                            }
                        });
        items.sort(
                Comparator.comparing((Map<String, Object> x) -> String.valueOf(x.get("module")))
                        .thenComparing(x -> String.valueOf(x.get("object")))
                        .thenComparing(x -> String.valueOf(x.get("method"))));
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<Map<String, String>>> createRole(
            @Valid @RequestBody RoleRequest req) {
        long operatorId = SecurityUtils.requireAdminId();
        String role = normalizeRole(req.role());
        ensureRole(role);
        recordAudit(operatorId, "role_create", role, null, null, Map.of("role", role));
        return ResponseEntity.ok(ApiResponse.success(Map.of("role", role)));
    }

    @DeleteMapping("/roles/{role}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable String role) {
        long operatorId = SecurityUtils.requireAdminId();
        String normalized = normalizeRole(decodeRoleParam(role));
        if (ROLE_ANCHOR.equals(normalized)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        authzRuleRepository.deleteByPtypeAndV0(PTYPE_POLICY, normalized);
        authzRuleRepository.deleteByPtypeAndV0AndV1(PTYPE_GROUP, normalized, ROLE_ANCHOR);
        authzRuleRepository.deleteByPtypeAndV0(PTYPE_GROUP, normalized);
        authzRuleRepository.deleteByPtypeAndV1(PTYPE_GROUP, normalized);
        recordAudit(operatorId, "role_delete", normalized, null, null, Map.of("role", normalized));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/roles/{role}/policies")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getRolePolicies(@PathVariable String role) {
        SecurityUtils.requireAdminId();
        String normalized = normalizeRole(decodeRoleParam(role));
        List<Map<String, String>> data =
                authzRuleRepository.findByPtypeAndV0(PTYPE_POLICY, normalized).stream()
                        .map(x -> policyRow(x.getV0(), x.getV1(), x.getV2()))
                        .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/policies")
    public ResponseEntity<ApiResponse<Void>> grantPolicy(@Valid @RequestBody PolicyRequest req) {
        long operatorId = SecurityUtils.requireAdminId();
        String role = ensureRole(normalizeRole(req.role()));
        String object = normalizeObject(req.object());
        String action = normalizeAction(req.action());
        if (authzRuleRepository.findByPtypeAndV0AndV1AndV2(PTYPE_POLICY, role, object, action).isEmpty()) {
            AuthzRuleEntity e = new AuthzRuleEntity();
            e.setPtype(PTYPE_POLICY);
            e.setV0(role);
            e.setV1(object);
            e.setV2(action);
            authzRuleRepository.save(e);
        }
        recordAudit(operatorId, "policy_grant", role, object, action, Map.of("role", role));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/policies")
    public ResponseEntity<ApiResponse<Void>> revokePolicy(@Valid @RequestBody PolicyRequest req) {
        long operatorId = SecurityUtils.requireAdminId();
        String role = normalizeRole(req.role());
        String object = normalizeObject(req.object());
        String action = normalizeAction(req.action());
        authzRuleRepository.findByPtypeAndV0AndV1AndV2(PTYPE_POLICY, role, object, action)
                .forEach(authzRuleRepository::delete);
        recordAudit(operatorId, "policy_revoke", role, object, action, Map.of("role", role));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/admins/{id}/roles")
    public ResponseEntity<ApiResponse<List<String>>> getAdminRoles(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        long adminId = parsePositiveLong(id, "admin_id_invalid");
        adminAccountRepository
                .findById(adminId)
                .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "admin_id_invalid"));
        return ResponseEntity.ok(ApiResponse.success(getAdminRolesInternal(adminId)));
    }

    @PutMapping("/admins/{id}/roles")
    public ResponseEntity<ApiResponse<Void>> setAdminRoles(
            @PathVariable String id, @RequestBody SetAdminRolesRequest req) {
        long operatorId = SecurityUtils.requireAdminId();
        long adminId = parsePositiveLong(id, "admin_id_invalid");
        adminAccountRepository
                .findById(adminId)
                .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "admin_id_invalid"));
        setAdminRolesInternal(adminId, req.roles() == null ? List.of() : req.roles());
        recordAudit(
                operatorId,
                "admin_roles_update",
                null,
                null,
                null,
                Map.of("target_admin_id", adminId, "roles", req.roles() == null ? List.of() : req.roles()));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String ensureRole(String role) {
        List<AuthzRuleEntity> existing = authzRuleRepository.findByPtypeAndV0(PTYPE_GROUP, role);
        for (AuthzRuleEntity e : existing) {
            if (ROLE_ANCHOR.equals(e.getV1())) {
                return role;
            }
        }
        AuthzRuleEntity e = new AuthzRuleEntity();
        e.setPtype(PTYPE_GROUP);
        e.setV0(role);
        e.setV1(ROLE_ANCHOR);
        authzRuleRepository.save(e);
        return role;
    }

    private List<String> listRolesInternal() {
        Set<String> roles = new TreeSet<>();
        for (AuthzRuleEntity e : authzRuleRepository.findByPtype(PTYPE_GROUP)) {
            if (isRole(e.getV0()) && !ROLE_ANCHOR.equals(e.getV0())) {
                roles.add(e.getV0());
            }
            if (isRole(e.getV1()) && !ROLE_ANCHOR.equals(e.getV1())) {
                roles.add(e.getV1());
            }
        }
        return new ArrayList<>(roles);
    }

    private List<String> getAdminRolesInternal(long adminId) {
        String subject = subjectForAdmin(adminId);
        List<String> roles =
                authzRuleRepository.findByPtypeAndV0(PTYPE_GROUP, subject).stream()
                        .map(AuthzRuleEntity::getV1)
                        .filter(AdminAuthzController::isRole)
                        .filter(r -> !ROLE_ANCHOR.equals(r))
                        .sorted()
                        .toList();
        if (!roles.isEmpty()) {
            return roles;
        }
        AdminAccount admin = adminAccountRepository.findById(adminId).orElse(null);
        if (admin != null && admin.isSuperAdmin()) {
            return List.of("role:system_admin");
        }
        return List.of("role:readonly_auditor");
    }

    private void setAdminRolesInternal(long adminId, List<String> roles) {
        String subject = subjectForAdmin(adminId);
        authzRuleRepository.deleteByPtypeAndV0(PTYPE_GROUP, subject);
        for (String roleRaw : roles) {
            String role = ensureRole(normalizeRole(roleRaw));
            AuthzRuleEntity e = new AuthzRuleEntity();
            e.setPtype(PTYPE_GROUP);
            e.setV0(subject);
            e.setV1(role);
            authzRuleRepository.save(e);
        }
    }

    private List<Map<String, String>> getAdminPoliciesInternal(long adminId) {
        String subject = subjectForAdmin(adminId);
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        authzRuleRepository.findByPtypeAndV0(PTYPE_POLICY, subject)
                .forEach(x -> out.put(keyOf(x.getV0(), x.getV1(), x.getV2()), policyRow(x.getV0(), x.getV1(), x.getV2())));

        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(getAdminRolesInternal(adminId));
        List<AuthzRuleEntity> gRules = authzRuleRepository.findByPtype(PTYPE_GROUP);
        while (!queue.isEmpty()) {
            String role = queue.removeFirst();
            if (!visited.add(role)) {
                continue;
            }
            authzRuleRepository.findByPtypeAndV0(PTYPE_POLICY, role)
                    .forEach(x -> out.put(keyOf(x.getV0(), x.getV1(), x.getV2()), policyRow(x.getV0(), x.getV1(), x.getV2())));
            for (AuthzRuleEntity g : gRules) {
                if (role.equals(g.getV0()) && isRole(g.getV1()) && !ROLE_ANCHOR.equals(g.getV1())) {
                    queue.addLast(g.getV1());
                }
            }
        }
        return out.values().stream()
                .sorted(
                        Comparator.comparing((Map<String, String> x) -> x.get("subject"))
                                .thenComparing(x -> x.get("object"))
                                .thenComparing(x -> x.get("action")))
                .toList();
    }

    private void recordAudit(
            long operatorAdminId,
            String action,
            String role,
            String object,
            String method,
            Map<String, Object> detail) {
        AuthzAuditLogEntity e = new AuthzAuditLogEntity();
        e.setOperatorAdminId(operatorAdminId);
        AdminAccount operator = adminAccountRepository.findById(operatorAdminId).orElse(null);
        e.setOperatorUsername(operator == null ? "" : operator.getUsername());
        e.setAction(action);
        e.setRole(role);
        e.setObject(object);
        e.setMethod(method);
        e.setCreatedAt(Instant.now());
        if (detail != null) {
            try {
                e.setDetailJson(objectMapper.writeValueAsString(detail));
            } catch (JsonProcessingException ignored) {
                e.setDetailJson("{}");
            }
        }
        authzAuditLogRepository.save(e);
    }

    private static String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().replace(" ", "_");
        if (normalized.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        return normalized.startsWith(ROLE_PREFIX) ? normalized : ROLE_PREFIX + normalized;
    }

    private static String normalizeObject(String object) {
        String normalized = object == null ? "" : object.trim();
        if (normalized.isEmpty()) {
            return "/";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.startsWith("/api/v1/")) {
            normalized = normalized.substring("/api/v1".length());
        } else if ("/api/v1".equals(normalized)) {
            normalized = "/";
        }
        return normalized;
    }

    private static String normalizeAction(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        return normalized;
    }

    private static String decodeRoleParam(String role) {
        return URLDecoder.decode(role, StandardCharsets.UTF_8).trim();
    }

    private static String subjectForAdmin(long adminId) {
        return "admin:" + adminId;
    }

    private static boolean isRole(String s) {
        return s != null && s.startsWith(ROLE_PREFIX);
    }

    private static String keyOf(String s, String o, String a) {
        return s + "|" + o + "|" + a;
    }

    private static Map<String, String> policyRow(String s, String o, String a) {
        return Map.of("subject", s, "object", o, "action", a);
    }

    private static boolean notBlank(String x) {
        return x != null && !x.isBlank();
    }

    private static Instant parseInstantNullable(String raw) {
        if (!notBlank(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    private static Object parseJsonOrNull(String json) {
        if (!notBlank(json)) {
            return null;
        }
        return json;
    }

    private static long parsePositiveLong(String raw, String errMsg) {
        try {
            long v = Long.parseLong(raw);
            if (v <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, errMsg);
            }
            return v;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, errMsg);
        }
    }

    private String deriveModule(String object) {
        String s = object.startsWith("/") ? object.substring(1) : object;
        if (s.isEmpty()) {
            return "system";
        }
        String[] seg = s.split("/");
        if (seg.length <= 1) {
            return seg[0];
        }
        if (!"admin".equals(seg[0])) {
            return seg[0];
        }
        if ("authz".equals(seg[1])) {
            return "authz";
        }
        return seg[1];
    }

    private String normalizeUsername(String username) {
        String s = username == null ? "" : username.trim();
        if (s.length() < 3 || s.length() > 64 || s.contains(" ")) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "admin_username_invalid");
        }
        return s;
    }

    private Map<String, Object> adminRow(AdminAccount admin) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", admin.getId());
        row.put("username", admin.getUsername());
        row.put("is_super", admin.isSuperAdmin());
        row.put("last_login_at", null);
        row.put("created_at", null);
        row.put("roles", getAdminRolesInternal(admin.getId()));
        return row;
    }

    private record CreateAdminRequest(@NotBlank String username, @NotBlank String password, Boolean isSuper) {}

    private record UpdateAdminRequest(String username, String password, Boolean isSuper) {}

    private record RoleRequest(@NotBlank String role) {}

    private record PolicyRequest(
            @NotBlank String role, @NotBlank String object, @NotBlank String action) {}

    private record SetAdminRolesRequest(List<String> roles) {}

    private record PolicySeed(String object, String action) {}
}
