package com.dujiao.api.dto.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CouponUpsertRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 200) String name,
        @JsonProperty("discount_percent") BigDecimal discountPercent,
        @JsonProperty("is_active") Boolean active) {}
