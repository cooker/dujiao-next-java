package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CapturePaymentResponseDto(
        @JsonProperty("payment_id") long paymentId, @JsonProperty("status") String status) {}
