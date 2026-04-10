package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ChangeEmailRequest(
        @Email @NotBlank @JsonProperty("new_email") String newEmail,
        @JsonProperty("old_code") String oldCode,
        @JsonProperty("new_code") @NotBlank String newCode) {}
