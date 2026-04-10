package com.dujiao.api.dto.order;

import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 创建订单并支付接口的通用响应（与 Go {@code CreateOrderAndPay} 成功体字段对齐；失败时 {@code
 * payment_error}）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderAndPayResponse(
        OrderDetailDto order,
        @JsonProperty("order_no") String orderNo,
        @JsonProperty("payment_error") String paymentError,
        @JsonProperty("order_paid") Boolean orderPaid,
        @JsonProperty("wallet_paid_amount") BigDecimal walletPaidAmount,
        @JsonProperty("online_pay_amount") BigDecimal onlinePayAmount,
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("provider_type") String providerType,
        @JsonProperty("channel_type") String channelType,
        @JsonProperty("interaction_mode") String interactionMode,
        @JsonProperty("pay_url") String payUrl,
        @JsonProperty("qr_code") String qrCode,
        @JsonProperty("expires_at") Instant expiresAt,
        @JsonProperty("channel_name") String channelName) {

    public static OrderAndPayResponse orderOnly(OrderDetailDto order, String orderNo) {
        return new OrderAndPayResponse(
                order, orderNo, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static OrderAndPayResponse withPaymentError(OrderDetailDto order, String orderNo, String paymentError) {
        return new OrderAndPayResponse(
                order, orderNo, paymentError, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static OrderAndPayResponse withPayment(OrderDetailDto order, String orderNo, CreatePaymentResponseDto p) {
        if (p == null) {
            return orderOnly(order, orderNo);
        }
        return new OrderAndPayResponse(
                order,
                orderNo,
                null,
                p.orderPaid(),
                p.walletPaidAmount(),
                p.onlinePayAmount(),
                p.paymentId(),
                p.providerType(),
                p.channelType(),
                p.interactionMode(),
                p.payUrl(),
                p.qrCode(),
                p.expiresAt(),
                p.channelName());
    }
}
