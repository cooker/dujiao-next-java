package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.PaymentEntity;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.dto.payment.CapturePaymentResponseDto;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.payment.GuestCapturePaymentRequest;
import com.dujiao.api.dto.payment.GuestCreatePaymentRequest;
import com.dujiao.api.dto.payment.LatestPaymentResponseDto;
import com.dujiao.api.repository.PaymentRepository;
import com.dujiao.api.repository.ShopOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 游客支付：在线支付单创建与查询（与 Go 行为对齐）。 */
@Service
public class GuestPaymentService {

    private final GuestOrderService guestOrderService;
    private final OrderOnlinePaymentService orderOnlinePaymentService;
    private final PaymentRepository paymentRepository;
    private final ShopOrderRepository shopOrderRepository;

    public GuestPaymentService(
            GuestOrderService guestOrderService,
            OrderOnlinePaymentService orderOnlinePaymentService,
            PaymentRepository paymentRepository,
            ShopOrderRepository shopOrderRepository) {
        this.guestOrderService = guestOrderService;
        this.orderOnlinePaymentService = orderOnlinePaymentService;
        this.paymentRepository = paymentRepository;
        this.shopOrderRepository = shopOrderRepository;
    }

    @Transactional
    public CreatePaymentResponseDto createPayment(GuestCreatePaymentRequest req, String clientIp) {
        ShopOrder order =
                guestOrderService.requireGuestOrder(
                        req.orderNo(), req.email(), req.orderPassword());
        return orderOnlinePaymentService.createOnlineForOrder(order, req.channelId(), clientIp);
    }

    @Transactional(readOnly = true)
    public CapturePaymentResponseDto capturePayment(long paymentId, GuestCapturePaymentRequest req) {
        if (paymentId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "payment_invalid");
        }
        PaymentEntity p =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "payment_not_found"));
        ShopOrder order =
                shopOrderRepository
                        .findById(p.getOrderId())
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        guestOrderService.requireGuestOrder(order.getOrderNo(), req.email(), req.orderPassword());
        return orderOnlinePaymentService.captureForGuest(paymentId, order);
    }

    @Transactional(readOnly = true)
    public LatestPaymentResponseDto latestPendingPayment(String email, String orderPassword, String orderNo) {
        ShopOrder order = guestOrderService.requireGuestOrder(orderNo, email, orderPassword);
        return orderOnlinePaymentService.latestForGuest(order);
    }
}
