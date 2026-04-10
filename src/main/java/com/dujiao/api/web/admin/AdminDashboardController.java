package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminDashboardService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.overview()));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trends() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.trends()));
    }

    @GetMapping("/rankings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rankings() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.rankings()));
    }

    @GetMapping("/inventory-alerts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> inventoryAlerts() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.inventoryAlerts()));
    }
}
