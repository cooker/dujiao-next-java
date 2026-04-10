package com.dujiao.api.service;

import com.dujiao.api.dto.payment.CapturePaymentResponseDto;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.payment.LatestPaymentResponseDto;
import com.dujiao.api.dto.payment.UserCreatePaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录用户支付：余额全额或在线支付单（与 Go {@code CreatePayment} 响应对齐）。 */
@Service
public class UserPaymentService {

    private final OrderOnlinePaymentService orderOnlinePaymentService;

    public UserPaymentService(OrderOnlinePaymentService orderOnlinePaymentService) {
        this.orderOnlinePaymentService = orderOnlinePaymentService;
    }

    @Transactional
    public CreatePaymentResponseDto createPayment(long userId, UserCreatePaymentRequest req, String clientIp) {
        return orderOnlinePaymentService.createPaymentForUser(userId, req, clientIp);
    }

    @Transactional(readOnly = true)
    public CapturePaymentResponseDto capturePayment(long userId, long paymentId) {
        return orderOnlinePaymentService.captureForUser(userId, paymentId);
    }

    @Transactional(readOnly = true)
    public LatestPaymentResponseDto latestPayment(long userId, String orderNo) {
        return orderOnlinePaymentService.latestForUser(userId, orderNo);
    }
}
