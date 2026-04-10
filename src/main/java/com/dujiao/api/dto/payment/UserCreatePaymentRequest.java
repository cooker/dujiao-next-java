package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code CreatePaymentRequest} 对齐。 */
public record UserCreatePaymentRequest(
        @NotBlank @JsonProperty("order_no") String orderNo,
        @JsonProperty("channel_id") long channelId,
        @JsonProperty("use_balance") boolean useBalance) {}
