package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @Email @NotBlank String email,
        @JsonProperty("verify_code") @NotBlank String verifyCode,
        @JsonProperty("new_password") @NotBlank @Size(min = 8, max = 128) String newPassword) {}
