package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GuestCapturePaymentRequest(
        @Email @NotBlank String email,
        @NotBlank @JsonProperty("order_password") String orderPassword) {}
