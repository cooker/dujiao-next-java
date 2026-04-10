package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.dto.admin.AdminLoginLogDto;
import com.dujiao.api.dto.admin.AdminSetMemberLevelRequest;
import com.dujiao.api.dto.admin.AdminUserBatchStatusRequest;
import com.dujiao.api.dto.admin.AdminUserDetailDto;
import com.dujiao.api.dto.admin.AdminUserUpdateRequest;
import com.dujiao.api.dto.admin.AdminWalletAdjustRequest;
import com.dujiao.api.dto.wallet.WalletDto;
import com.dujiao.api.dto.wallet.WalletRechargeAdminDto;
import com.dujiao.api.dto.wallet.WalletTransactionDto;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminUserService;
import com.dujiao.api.service.AdminWalletRechargeService;
import com.dujiao.api.service.UserLoginLogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminWalletRechargeService adminWalletRechargeService;
    private final UserLoginLogService userLoginLogService;

    public AdminUserController(
            AdminUserService adminUserService,
            AdminWalletRechargeService adminWalletRechargeService,
            UserLoginLogService userLoginLogService) {
        this.adminUserService = adminUserService;
        this.adminWalletRechargeService = adminWalletRechargeService;
        this.userLoginLogService = userLoginLogService;
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<List<AdminUserDetailDto>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(adminUserService.list(page, pageSize));
    }

    @GetMapping("/user-login-logs")
    public ResponseEntity<PageResponse<List<AdminLoginLogDto>>> loginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "user_id", required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(name = "fail_reason", required = false) String failReason,
            @RequestParam(name = "client_ip", required = false) String clientIp,
            @RequestParam(name = "created_from", required = false) String createdFrom,
            @RequestParam(name = "created_to", required = false) String createdTo) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                userLoginLogService.listForAdmin(
                        page,
                        pageSize,
                        userId,
                        email,
                        status,
                        failReason,
                        clientIp,
                        UserLoginLogService.parseInstantQuery(createdFrom),
                        UserLoginLogService.parseInstantQuery(createdTo)));
    }

    @PutMapping("/users/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchStatus(
            @Valid @RequestBody AdminUserBatchStatusRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminUserService.batchUpdateStatus(req)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUser(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminUserService.get(Long.parseLong(id))));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> updateUser(
            @PathVariable String id, @Valid @RequestBody AdminUserUpdateRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminUserService.update(Long.parseLong(id), req)));
    }

    @GetMapping("/users/{id}/coupon-usages")
    public ResponseEntity<ApiResponse<List<Object>>> couponUsages(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminUserService.get(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/users/{id}/wallet")
    public ResponseEntity<ApiResponse<WalletDto>> userWallet(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminUserService.userWallet(Long.parseLong(id))));
    }

    @GetMapping("/users/{id}/wallet/transactions")
    public ResponseEntity<PageResponse<List<WalletTransactionDto>>> walletTransactions(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                adminUserService.userWalletTransactions(Long.parseLong(id), page, pageSize));
    }

    @PostMapping("/users/{id}/wallet/adjust")
    public ResponseEntity<ApiResponse<Void>> adjustWallet(
            @PathVariable String id, @Valid @RequestBody AdminWalletAdjustRequest req) {
        SecurityUtils.requireAdminId();
        adminUserService.adjustUserWallet(Long.parseLong(id), req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/users/{id}/member-level")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> setMemberLevel(
            @PathVariable String id, @Valid @RequestBody AdminSetMemberLevelRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminUserService.setMemberLevel(Long.parseLong(id), req)));
    }

    @GetMapping("/wallet/recharges")
    public ResponseEntity<PageResponse<List<WalletRechargeAdminDto>>> walletRecharges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(adminWalletRechargeService.list(page, pageSize));
    }
}
