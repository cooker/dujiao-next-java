package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        String displayName,
        @JsonProperty("agree_terms") Boolean agreeTerms,
        @JsonProperty("verify_code") String verifyCode) {}
