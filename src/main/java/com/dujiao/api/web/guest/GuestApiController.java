package com.dujiao.api.web.guest;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.dto.payment.CapturePaymentResponseDto;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.payment.LatestPaymentResponseDto;
import com.dujiao.api.dto.order.CreateGuestOrderAndPayRequest;
import com.dujiao.api.dto.order.CreateGuestOrderRequest;
import com.dujiao.api.dto.order.OrderAndPayResponse;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.dto.payment.GuestCapturePaymentRequest;
import com.dujiao.api.dto.payment.GuestCreatePaymentRequest;
import com.dujiao.api.service.GuestOrderService;
import com.dujiao.api.service.GuestPaymentService;
import com.dujiao.api.service.OrderFulfillmentService;
import com.dujiao.api.web.FulfillmentHttpResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/guest")
public class GuestApiController {

    private final GuestOrderService guestOrderService;
    private final GuestPaymentService guestPaymentService;
    private final OrderFulfillmentService orderFulfillmentService;

    public GuestApiController(
            GuestOrderService guestOrderService,
            GuestPaymentService guestPaymentService,
            OrderFulfillmentService orderFulfillmentService) {
        this.guestOrderService = guestOrderService;
        this.guestPaymentService = guestPaymentService;
        this.orderFulfillmentService = orderFulfillmentService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDetailDto>> createOrder(
            @Valid @RequestBody CreateGuestOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.success(guestOrderService.create(req)));
    }

    @PostMapping("/orders/create-and-pay")
    public ResponseEntity<ApiResponse<OrderAndPayResponse>> createAndPay(
            HttpServletRequest http, @Valid @RequestBody CreateGuestOrderAndPayRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(guestOrderService.createAndPay(req, clientIp(http))));
    }

    @PostMapping("/orders/preview")
    public ResponseEntity<ApiResponse<OrderDetailDto>> previewOrder(
            @Valid @RequestBody CreateGuestOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.success(guestOrderService.preview(req)));
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<List<OrderDetailDto>>> listOrders(
            @RequestParam String email,
            @RequestParam(name = "order_password") String orderPassword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "order_no", required = false) String orderNo,
            @RequestParam(name = "status", required = false) String status) {
        requireGuestCredentials(email, orderPassword);
        return ResponseEntity.ok(
                guestOrderService.list(email, orderPassword, page, pageSize, orderNo, status));
    }

    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrder(
            @PathVariable("orderNo") String orderNo,
            @RequestParam String email,
            @RequestParam(name = "order_password") String orderPassword) {
        requireGuestCredentials(email, orderPassword);
        return ResponseEntity.ok(
                ApiResponse.success(
                        guestOrderService.getByOrderNo(orderNo, email, orderPassword)));
    }

    @GetMapping("/orders/{orderNo}/fulfillment/download")
    public ResponseEntity<byte[]> downloadFulfillment(
            @PathVariable String orderNo,
            @RequestParam String email,
            @RequestParam(name = "order_password") String orderPassword) {
        requireGuestCredentials(email, orderPassword);
        byte[] body = orderFulfillmentService.downloadForGuest(orderNo, email, orderPassword);
        return FulfillmentHttpResponses.plaintextAttachment(orderNo, body);
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<CreatePaymentResponseDto>> createPayment(
            HttpServletRequest http, @Valid @RequestBody GuestCreatePaymentRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(guestPaymentService.createPayment(req, clientIp(http))));
    }

    @PostMapping("/payments/{id}/capture")
    public ResponseEntity<ApiResponse<CapturePaymentResponseDto>> capturePayment(
            @PathVariable String id, @Valid @RequestBody GuestCapturePaymentRequest req) {
        long paymentId;
        try {
            paymentId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "payment_invalid");
        }
        return ResponseEntity.ok(ApiResponse.success(guestPaymentService.capturePayment(paymentId, req)));
    }

    @GetMapping("/payments/latest")
    public ResponseEntity<ApiResponse<LatestPaymentResponseDto>> latestPayment(
            @RequestParam String email,
            @RequestParam(name = "order_password") String orderPassword,
            @RequestParam(name = "order_no") String orderNo) {
        requireGuestCredentials(email, orderPassword);
        return ResponseEntity.ok(
                ApiResponse.success(
                        guestPaymentService.latestPendingPayment(email, orderPassword, orderNo)));
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String ra = req.getRemoteAddr();
        return ra == null ? "" : ra;
    }

    /** 与 Go {@code ListGuestOrders} / {@code GetGuestOrderByOrderNo} 一致：缺少邮箱或查询密码时返回明确错误码。 */
    private static void requireGuestCredentials(String email, String orderPassword) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "guest_email_required");
        }
        if (orderPassword == null || orderPassword.isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "guest_password_required");
        }
    }
}
