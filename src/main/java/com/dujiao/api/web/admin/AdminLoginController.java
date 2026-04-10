package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.auth.AdminLoginRequest;
import com.dujiao.api.dto.auth.AdminLoginResponse;
import com.dujiao.api.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/admin")
public class AdminLoginController {

    private final AdminAuthService adminAuthService;

    public AdminLoginController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminAuthService.login(req)));
    }
}
