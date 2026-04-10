package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 与 Go {@code LatestPaymentResp} 对齐。 */
public record LatestPaymentResponseDto(
        @JsonProperty("payment_id") long paymentId,
        @JsonProperty("order_no") String orderNo,
        @JsonProperty("channel_id") long channelId,
        @JsonProperty("channel_name") String channelName,
        @JsonProperty("provider_type") String providerType,
        @JsonProperty("channel_type") String channelType,
        @JsonProperty("interaction_mode") String interactionMode,
        @JsonProperty("pay_url") String payUrl,
        @JsonProperty("qr_code") String qrCode,
        @JsonProperty("expires_at") Instant expiresAt) {}
