package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(max = 200) @JsonProperty("display_name") String displayName,
        @Pattern(regexp = "^(active|disabled|suspended)$") String status) {}
