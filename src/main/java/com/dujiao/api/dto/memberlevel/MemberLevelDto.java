package com.dujiao.api.dto.memberlevel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record MemberLevelDto(
        long id,
        String slug,
        String name,
        @JsonProperty("discount_rate") BigDecimal discountRate,
        @JsonProperty("recharge_threshold") BigDecimal rechargeThreshold,
        @JsonProperty("spend_threshold") BigDecimal spendThreshold,
        @JsonProperty("is_default") boolean defaultLevel,
        @JsonProperty("sort_order") int sortOrder,
        @JsonProperty("is_active") boolean active) {}
