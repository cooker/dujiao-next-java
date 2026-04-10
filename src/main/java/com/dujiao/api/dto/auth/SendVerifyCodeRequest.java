package com.dujiao.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerifyCodeRequest(@Email @NotBlank String email, @NotBlank String purpose) {}
