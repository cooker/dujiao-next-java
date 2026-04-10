package com.dujiao.api.web.admin;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.dto.admin.AdminCreateFulfillmentRequest;
import com.dujiao.api.dto.order.AdminOrderStatusPatchRequest;
import com.dujiao.api.dto.order.FulfillmentDto;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AdminShopOrderService;
import com.dujiao.api.service.OrderFulfillmentService;
import com.dujiao.api.web.FulfillmentHttpResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/admin")
public class AdminOrderController {

    private final AdminShopOrderService adminShopOrderService;
    private final OrderFulfillmentService orderFulfillmentService;

    public AdminOrderController(
            AdminShopOrderService adminShopOrderService, OrderFulfillmentService orderFulfillmentService) {
        this.adminShopOrderService = adminShopOrderService;
        this.orderFulfillmentService = orderFulfillmentService;
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<List<OrderDetailDto>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(adminShopOrderService.list(page, pageSize));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrder(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(adminShopOrderService.get(Long.parseLong(id))));
    }

    @GetMapping("/orders/{id}/fulfillment/download")
    public ResponseEntity<byte[]> downloadFulfillment(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        long orderId = Long.parseLong(id);
        OrderFulfillmentService.FulfillmentDownloadResult r =
                orderFulfillmentService.downloadForAdmin(orderId);
        return FulfillmentHttpResponses.plaintextAttachment(r.orderNo(), r.body());
    }

    @PatchMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> updateOrderStatus(
            @PathVariable String id, @Valid @RequestBody AdminOrderStatusPatchRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminShopOrderService.updateStatus(Long.parseLong(id), req)));
    }

    @PostMapping("/orders/{id}/refund-to-wallet")
    public ResponseEntity<ApiResponse<OrderDetailDto>> refundToWallet(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(adminShopOrderService.refundToWallet(Long.parseLong(id))));
    }

    @PostMapping("/fulfillments")
    public ResponseEntity<ApiResponse<FulfillmentDto>> createFulfillment(
            @Valid @RequestBody AdminCreateFulfillmentRequest req) {
        long adminId = SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(orderFulfillmentService.createManual(adminId, req)));
    }
}
