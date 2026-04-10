package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.auth.AdminChangePasswordRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminPasswordController {

    private final AdminAuthService adminAuthService;

    public AdminPasswordController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @Valid @RequestBody AdminChangePasswordRequest req) {
        long adminId = SecurityUtils.requireAdminId();
        adminAuthService.changePassword(adminId, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
