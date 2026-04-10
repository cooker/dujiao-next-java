package com.dujiao.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminChangePasswordRequest(
        @NotBlank @JsonProperty("old_password") String oldPassword,
        @NotBlank @Size(min = 8, max = 128) @JsonProperty("new_password") String newPassword) {}
