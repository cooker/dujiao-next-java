package com.dujiao.api.dto.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CouponDto(
        long id,
        String code,
        String name,
        @JsonProperty("discount_percent") BigDecimal discountPercent,
        @JsonProperty("is_active") boolean active) {}
