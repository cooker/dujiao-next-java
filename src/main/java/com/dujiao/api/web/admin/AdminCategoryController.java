package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.category.AdminCategoryDto;
import com.dujiao.api.dto.category.CategoryUpsertRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminCategoryService;
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
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCategoryDto>>> list() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCategoryService.list()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminCategoryDto>> create(
            @Valid @RequestBody CategoryUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCategoryService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCategoryDto>> update(
            @PathVariable String id, @Valid @RequestBody CategoryUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminCategoryService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminCategoryService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
