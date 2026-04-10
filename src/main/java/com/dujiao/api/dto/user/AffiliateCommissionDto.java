package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record AffiliateCommissionDto(
        long id,
        @JsonProperty("commission_type") String commissionType,
        @JsonProperty("commission_amount") BigDecimal commissionAmount,
        String status,
        @JsonProperty("confirm_at") Instant confirmAt,
        @JsonProperty("available_at") Instant availableAt,
        @JsonProperty("created_at") Instant createdAt) {}
