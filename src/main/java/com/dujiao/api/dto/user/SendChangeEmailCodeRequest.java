package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SendChangeEmailCodeRequest(
        @NotBlank String kind,
        @JsonProperty("new_email") String newEmail) {}
