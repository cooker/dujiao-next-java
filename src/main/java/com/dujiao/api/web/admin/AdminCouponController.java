package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<CouponDto>>> listCoupons() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.list()));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponDto>> updateCoupon(
            @PathVariable String id, @Valid @RequestBody CouponUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminCouponService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(
            @Valid @RequestBody PromotionUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPromotionService.create(req)));
    }

    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> listPromotions() {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPromotionService.list()));
    }

    @PutMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(
            @PathVariable String id, @Valid @RequestBody PromotionUpsertRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminPromotionService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        adminPromotionService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
