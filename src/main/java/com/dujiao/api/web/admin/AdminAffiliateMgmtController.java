package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.AffiliateCommissionEntity;
import com.dujiao.api.domain.AffiliateWithdrawEntity;
import com.dujiao.api.dto.admin.AffiliateProfileStatusRequest;
import com.dujiao.api.dto.admin.BatchAffiliateProfileStatusRequest;
import com.dujiao.api.repository.AffiliateCommissionRepository;
import com.dujiao.api.repository.AffiliateWithdrawRepository;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminAffiliateMgmtService;
import com.dujiao.api.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    private static final String WITHDRAW_PENDING = "pending_review";
    private static final String WITHDRAW_REJECTED = "rejected";
    private static final String WITHDRAW_PAID = "paid";
    private static final String COMMISSION_AVAILABLE = "available";

    private final AdminAffiliateMgmtService adminAffiliateMgmtService;
    private final AffiliateCommissionRepository affiliateCommissionRepository;
    private final AffiliateWithdrawRepository affiliateWithdrawRepository;

    public AdminAffiliateMgmtController(
            AdminAffiliateMgmtService adminAffiliateMgmtService,
            AffiliateCommissionRepository affiliateCommissionRepository,
            AffiliateWithdrawRepository affiliateWithdrawRepository) {
        this.adminAffiliateMgmtService = adminAffiliateMgmtService;
        this.affiliateCommissionRepository = affiliateCommissionRepository;
        this.affiliateWithdrawRepository = affiliateWithdrawRepository;
    }

    /** 与 Go {@code ListAffiliateUsers} 一致：分页 + {@code user_id}/{@code status}/{@code code}/{@code keyword}。 */
    @GetMapping("/users")
    public ResponseEntity<PageResponse<List<Map<String, Object>>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "user_id", required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String keyword) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                adminAffiliateMgmtService.listUsers(page, pageSize, userId, status, code, keyword));
    }

    /** 与 Go {@code UpdateAffiliateUserStatus} 一致：路径 {@code id} 为推广档案 ID（非用户 ID）。 */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable String id, @Valid @RequestBody AffiliateProfileStatusRequest req) {
        SecurityUtils.requireAdminId();
        long profileId = parsePositiveLong(id);
        return ResponseEntity.ok(
                ApiResponse.success(adminAffiliateMgmtService.updateProfileStatus(profileId, req)));
    }

    /** 与 Go {@code BatchUpdateAffiliateUserStatus} 一致：{@code profile_ids}。 */
    @PatchMapping("/users/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUserStatus(
            @Valid @RequestBody BatchAffiliateProfileStatusRequest req) {
        SecurityUtils.requireAdminId();
        long updated = adminAffiliateMgmtService.batchUpdateProfileStatus(req);
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
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
            }
            return x;
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
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
