package com.dujiao.api.dto.memberlevel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MemberLevelUpsertRequest(
        @NotBlank @Size(max = 64) String slug,
        @NotBlank @Size(max = 120) String name,
        @JsonProperty("discount_rate") BigDecimal discountRate,
        @JsonProperty("recharge_threshold") BigDecimal rechargeThreshold,
        @JsonProperty("spend_threshold") BigDecimal spendThreshold,
        @JsonProperty("is_default") Boolean defaultLevel,
        @JsonProperty("sort_order") Integer sortOrder,
        @JsonProperty("is_active") Boolean active) {}
