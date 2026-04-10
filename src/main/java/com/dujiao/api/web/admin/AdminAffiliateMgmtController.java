package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.AffiliateCommissionEntity;
import com.dujiao.api.domain.AffiliateProfileEntity;
import com.dujiao.api.domain.AffiliateWithdrawEntity;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.repository.AffiliateCommissionRepository;
import com.dujiao.api.repository.AffiliateProfileRepository;
import com.dujiao.api.repository.AffiliateWithdrawRepository;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.web.ApiPaths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/affiliates")
public class AdminAffiliateMgmtController {

    private static final String PROFILE_ACTIVE = "active";
    private static final String PROFILE_DISABLED = "disabled";
    private static final String WITHDRAW_PENDING = "pending_review";
    private static final String WITHDRAW_REJECTED = "rejected";
    private static final String WITHDRAW_PAID = "paid";
    private static final String COMMISSION_AVAILABLE = "available";

    private final AffiliateProfileRepository affiliateProfileRepository;
    private final AffiliateCommissionRepository affiliateCommissionRepository;
    private final AffiliateWithdrawRepository affiliateWithdrawRepository;
    private final UserAccountRepository userAccountRepository;

    public AdminAffiliateMgmtController(
            AffiliateProfileRepository affiliateProfileRepository,
            AffiliateCommissionRepository affiliateCommissionRepository,
            AffiliateWithdrawRepository affiliateWithdrawRepository,
            UserAccountRepository userAccountRepository) {
        this.affiliateProfileRepository = affiliateProfileRepository;
        this.affiliateCommissionRepository = affiliateCommissionRepository;
        this.affiliateWithdrawRepository = affiliateWithdrawRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<List<Map<String, Object>>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        SecurityUtils.requireAdminId();
        int p = Math.max(1, page);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        String st = status == null ? "" : status.trim().toLowerCase();
        Page<AffiliateProfileEntity> rows =
                st.isEmpty()
                        ? affiliateProfileRepository.findAllByOrderByIdDesc(pr)
                        : affiliateProfileRepository.findByStatusOrderByIdDesc(st, pr);
        Map<Long, UserAccount> users = usersByIds(rows.getContent());
        List<Map<String, Object>> data =
                rows.getContent().stream().map(x -> profileRow(x, users.get(x.getUserId()))).toList();
        PaginationDto pg = PaginationDto.of(rows.getNumber() + 1, rows.getSize(), rows.getTotalElements());
        return ResponseEntity.ok(PageResponse.success(data, pg));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        SecurityUtils.requireAdminId();
        long userId = parsePositiveLong(id);
        String status = extractStatus(body);
        AffiliateProfileEntity profile =
                affiliateProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "affiliate_profile_not_found"));
        profile.setStatus(status);
        affiliateProfileRepository.save(profile);
        UserAccount u = userAccountRepository.findById(userId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(profileRow(profile, u)));
    }

    @PatchMapping("/users/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUserStatus(
            @RequestBody(required = false) Map<String, Object> body) {
        SecurityUtils.requireAdminId();
        if (body == null || !(body.get("ids") instanceof List<?> idsRaw)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        String status = extractStatus(body);
        int updated = 0;
        for (Object o : idsRaw) {
            if (o instanceof Number n && n.longValue() > 0) {
                affiliateProfileRepository
                        .findByUserId(n.longValue())
                        .ifPresent(
                                p -> {
                                    p.setStatus(status);
                                    affiliateProfileRepository.save(p);
                                });
                updated++;
            }
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated)));
    }

    @GetMapping("/commissions")
    public ResponseEntity<PageResponse<List<Map<String, Object>>>> commissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status) {
        SecurityUtils.requireAdminId();
        int p = Math.max(1, page);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        String st = status == null ? "" : status.trim().toLowerCase();
        Page<AffiliateCommissionEntity> rows =
                st.isEmpty()
                        ? affiliateCommissionRepository.findAllByOrderByIdDesc(pr)
                        : affiliateCommissionRepository.findByStatusOrderByIdDesc(st, pr);
        List<Map<String, Object>> data = rows.getContent().stream().map(this::commissionRow).toList();
        PaginationDto pg = PaginationDto.of(rows.getNumber() + 1, rows.getSize(), rows.getTotalElements());
        return ResponseEntity.ok(PageResponse.success(data, pg));
    }

    @GetMapping("/withdraws")
    public ResponseEntity<PageResponse<List<Map<String, Object>>>> withdraws(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status) {
        SecurityUtils.requireAdminId();
        int p = Math.max(1, page);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        String st = status == null ? "" : status.trim().toLowerCase();
        Page<AffiliateWithdrawEntity> rows =
                st.isEmpty()
                        ? affiliateWithdrawRepository.findAllByOrderByIdDesc(pr)
                        : affiliateWithdrawRepository.findByStatusOrderByIdDesc(st, pr);
        List<Map<String, Object>> data = rows.getContent().stream().map(this::withdrawRow).toList();
        PaginationDto pg = PaginationDto.of(rows.getNumber() + 1, rows.getSize(), rows.getTotalElements());
        return ResponseEntity.ok(PageResponse.success(data, pg));
    }

    @PostMapping("/withdraws/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectWithdraw(
            @PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        SecurityUtils.requireAdminId();
        long wid = parsePositiveLong(id);
        AffiliateWithdrawEntity w =
                affiliateWithdrawRepository
                        .findById(wid)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "withdraw_not_found"));
        if (!WITHDRAW_PENDING.equals(w.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "withdraw_status_invalid");
        }
        String reason = "";
        if (body != null && body.get("reason") instanceof String s) {
            reason = s.trim();
        }
        w.setStatus(WITHDRAW_REJECTED);
        w.setRejectReason(reason);
        affiliateWithdrawRepository.save(w);
        List<AffiliateCommissionEntity> related = affiliateCommissionRepository.findByWithdrawRequestId(wid);
        for (AffiliateCommissionEntity c : related) {
            c.setStatus(COMMISSION_AVAILABLE);
            c.setWithdrawRequestId(null);
            affiliateCommissionRepository.save(c);
        }
        return ResponseEntity.ok(ApiResponse.success(withdrawRow(w)));
    }

    @PostMapping("/withdraws/{id}/pay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payWithdraw(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        long wid = parsePositiveLong(id);
        AffiliateWithdrawEntity w =
                affiliateWithdrawRepository
                        .findById(wid)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "withdraw_not_found"));
        if (!WITHDRAW_PENDING.equals(w.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "withdraw_status_invalid");
        }
        w.setStatus(WITHDRAW_PAID);
        affiliateWithdrawRepository.save(w);
        return ResponseEntity.ok(ApiResponse.success(withdrawRow(w)));
    }

    private static long parsePositiveLong(String v) {
        try {
            long x = Long.parseLong(v);
            if (x <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
            }
            return x;
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
    }

    private static String extractStatus(Map<String, Object> body) {
        if (body == null || !(body.get("status") instanceof String s)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        String st = s.trim().toLowerCase();
        if (!PROFILE_ACTIVE.equals(st) && !PROFILE_DISABLED.equals(st)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        return st;
    }

    private Map<Long, UserAccount> usersByIds(List<AffiliateProfileEntity> profiles) {
        Set<Long> ids = profiles.stream().map(AffiliateProfileEntity::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<UserAccount> users = userAccountRepository.findAllById(ids);
        Map<Long, UserAccount> m = new HashMap<>();
        for (UserAccount u : users) {
            m.put(u.getId(), u);
        }
        return m;
    }

    private Map<String, Object> profileRow(AffiliateProfileEntity p, UserAccount u) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.getId());
        out.put("user_id", p.getUserId());
        out.put("affiliate_code", p.getAffiliateCode());
        out.put("status", p.getStatus());
        out.put("created_at", p.getCreatedAt());
        out.put("updated_at", p.getUpdatedAt());
        if (u != null) {
            out.put("user_email", u.getEmail());
            out.put("user_display_name", u.getDisplayName() == null ? "" : u.getDisplayName());
            out.put("user_status", u.getStatus());
        } else {
            out.put("user_email", "");
            out.put("user_display_name", "");
            out.put("user_status", "");
        }
        return out;
    }

    private Map<String, Object> withdrawRow(AffiliateWithdrawEntity w) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", w.getId());
        out.put("affiliate_profile_id", w.getAffiliateProfileId());
        out.put("amount", w.getAmount());
        out.put("channel", w.getChannel());
        out.put("account", w.getAccount());
        out.put("status", w.getStatus());
        out.put("reject_reason", w.getRejectReason() == null ? "" : w.getRejectReason());
        out.put("created_at", w.getCreatedAt());
        out.put("updated_at", w.getUpdatedAt());
        return out;
    }

    private Map<String, Object> commissionRow(AffiliateCommissionEntity c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", c.getId());
        out.put("affiliate_profile_id", c.getAffiliateProfileId());
        out.put("order_id", c.getOrderId() == null ? 0L : c.getOrderId());
        out.put("commission_type", c.getCommissionType());
        out.put("commission_amount", c.getCommissionAmount());
        out.put("status", c.getStatus());
        out.put("confirm_at", c.getConfirmAt());
        out.put("available_at", c.getAvailableAt());
        out.put("withdraw_request_id", c.getWithdrawRequestId() == null ? 0L : c.getWithdrawRequestId());
        out.put("invalid_reason", c.getInvalidReason() == null ? "" : c.getInvalidReason());
        out.put("created_at", c.getCreatedAt());
        return out;
    }
}
