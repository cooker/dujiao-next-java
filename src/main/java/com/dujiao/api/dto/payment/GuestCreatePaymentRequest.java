package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GuestCreatePaymentRequest(
        @Email @NotBlank String email,
        @NotBlank @JsonProperty("order_password") String orderPassword,
        @NotBlank @JsonProperty("order_no") String orderNo,
        @JsonProperty("channel_id") long channelId) {}
