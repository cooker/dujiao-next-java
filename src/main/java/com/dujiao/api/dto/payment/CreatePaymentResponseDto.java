package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/** 与 Go {@code CreatePaymentResp} JSON 对齐。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatePaymentResponseDto(
        @JsonProperty("order_paid") Boolean orderPaid,
        @JsonProperty("wallet_paid_amount") BigDecimal walletPaidAmount,
        @JsonProperty("online_pay_amount") BigDecimal onlinePayAmount,
        @JsonProperty("payment_id") Long paymentId,
        @JsonProperty("channel_id") Long channelId,
        @JsonProperty("provider_type") String providerType,
        @JsonProperty("channel_type") String channelType,
        @JsonProperty("interaction_mode") String interactionMode,
        @JsonProperty("pay_url") String payUrl,
        @JsonProperty("qr_code") String qrCode,
        @JsonProperty("expires_at") Instant expiresAt,
        @JsonProperty("channel_name") String channelName) {

    public static CreatePaymentResponseDto walletOnly(BigDecimal walletPaid) {
        return new CreatePaymentResponseDto(
                true,
                walletPaid,
                BigDecimal.ZERO.setScale(2),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
