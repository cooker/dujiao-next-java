package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.dto.coupon.CouponDto;
import com.dujiao.api.dto.coupon.CouponUpsertRequest;
import com.dujiao.api.dto.promotion.PromotionDto;
import com.dujiao.api.dto.promotion.PromotionUpsertRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminCouponService;
import com.dujiao.api.service.AdminPromotionService;
import com.dujiao.api.web.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 与 Go {@code admin_coupon.go}、{@code admin_promotion.go} 路由及响应形状对齐（分页列表、删除返回 {@code
 * deleted:true}）。
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;
    private final AdminPromotionService adminPromotionService;

    public AdminCouponController(
            AdminCouponService adminCouponService, AdminPromotionService adminPromotionService) {
        this.adminCouponService = adminCouponService;
        this.adminPromotionService = adminPromotionService;
    }

    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponDto>> createCoupon(@Valid @RequestBody CouponUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.create(req)));
    }

    @GetMapping("/coupons")
    public ResponseEntity<PageResponse<List<CouponDto>>> listCoupons(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String id,
            @RequestParam(name = "scope_ref_id", required = false) String scopeRefId,
            @RequestParam(name = "is_active", required = false) String isActive) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                adminCouponService.list(page, pageSize, code, id, scopeRefId, isActive));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponDto>> updateCoupon(
            @PathVariable String id, @Valid @RequestBody CouponUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminCouponService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCoupon(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminCouponService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true)));
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(
            @Valid @RequestBody PromotionUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPromotionService.create(req)));
    }

    @GetMapping("/promotions")
    public ResponseEntity<PageResponse<List<PromotionDto>>> listPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String id,
            @RequestParam(name = "scope_ref_id", required = false) String scopeRefId,
            @RequestParam(name = "is_active", required = false) String isActive) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                adminPromotionService.list(page, pageSize, id, scopeRefId, isActive));
    }

    @PutMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(
            @PathVariable String id, @Valid @RequestBody PromotionUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminPromotionService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePromotion(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminPromotionService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true)));
    }
}
