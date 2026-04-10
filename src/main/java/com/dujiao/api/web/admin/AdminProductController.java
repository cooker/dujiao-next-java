package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.dto.product.ProductBatchCategoryRequest;
import com.dujiao.api.dto.product.ProductBatchIdsRequest;
import com.dujiao.api.dto.product.ProductBatchStatusRequest;
import com.dujiao.api.dto.product.ProductDto;
import com.dujiao.api.dto.product.ProductQuickUpdateRequest;
import com.dujiao.api.dto.product.ProductUpsertRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminProductService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<java.util.List<ProductDto>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "category_id", required = false) String categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(name = "fulfillment_type", required = false) String fulfillmentType,
            @RequestParam(name = "stock_status", required = false) String stockStatus,
            @RequestParam(name = "stock_staus", required = false) String stockStaus) {
        SecurityUtils.requireAdminId();
        String stock = (stockStatus == null || stockStatus.isBlank()) ? stockStaus : stockStatus;
        var result =
                adminProductService.listAdmin(
                        categoryId, search, fulfillmentType, stock, 10, page, pageSize);
        return ResponseEntity.ok(
                PageResponse.success(
                        result.getContent(),
                        PaginationDto.of(page, pageSize, result.getTotalElements())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> get(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminProductService.get(Long.parseLong(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> create(@Valid @RequestBody ProductUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminProductService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> update(
            @PathVariable String id, @Valid @RequestBody ProductUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminProductService.update(Long.parseLong(id), req)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> quickUpdate(
            @PathVariable String id, @RequestBody ProductQuickUpdateRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminProductService.quickUpdate(Long.parseLong(id), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminProductService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchStatus(
            @Valid @RequestBody ProductBatchStatusRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminProductService.batchUpdateStatus(req)));
    }

    @PostMapping("/batch-category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchCategory(
            @Valid @RequestBody ProductBatchCategoryRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminProductService.batchUpdateCategory(req)));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDelete(
            @Valid @RequestBody ProductBatchIdsRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminProductService.batchDelete(req)));
    }
}
