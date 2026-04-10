package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminUserBatchStatusRequest(
        @JsonProperty("user_ids") @NotEmpty List<Long> userIds,
        @NotBlank @Size(max = 32) String status) {}
