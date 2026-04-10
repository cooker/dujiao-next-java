package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.banner.BannerDto;
import com.dujiao.api.dto.banner.BannerUpsertRequest;
import com.dujiao.api.service.AdminBannerService;
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
@RequestMapping(ApiPaths.V1 + "/admin/banners")
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    public AdminBannerController(AdminBannerService adminBannerService) {
        this.adminBannerService = adminBannerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(adminBannerService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerDto>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminBannerService.get(Long.parseLong(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BannerDto>> create(@Valid @RequestBody BannerUpsertRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminBannerService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerDto>> update(
            @PathVariable String id, @Valid @RequestBody BannerUpsertRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(adminBannerService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminBannerService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
