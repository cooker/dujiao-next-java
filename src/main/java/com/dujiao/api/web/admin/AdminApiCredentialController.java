package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.admin.ApiCredentialAdminDto;
import com.dujiao.api.dto.admin.ApiCredentialApproveResponse;
import com.dujiao.api.dto.admin.ApiCredentialStatusRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminApiCredentialService;
import com.dujiao.api.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/api-credentials")
public class AdminApiCredentialController {

    private final AdminApiCredentialService adminApiCredentialService;

    public AdminApiCredentialController(AdminApiCredentialService adminApiCredentialService) {
        this.adminApiCredentialService = adminApiCredentialService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiCredentialAdminDto>>> list() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminApiCredentialService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiCredentialAdminDto>> get(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminApiCredentialService.get(Long.parseLong(id))));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ApiCredentialApproveResponse>> approve(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminApiCredentialService.approve(Long.parseLong(id))));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ApiCredentialAdminDto>> reject(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminApiCredentialService.reject(Long.parseLong(id))));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApiCredentialAdminDto>> updateStatus(
            @PathVariable String id, @Valid @RequestBody ApiCredentialStatusRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminApiCredentialService.updateStatus(Long.parseLong(id), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminApiCredentialService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
