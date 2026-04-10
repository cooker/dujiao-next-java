package com.dujiao.api.dto.coupon;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** 与 Go {@code CreateCouponRequest} 对齐。 */
public record CouponUpsertRequest(
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(max = 32) String type,
        @NotNull BigDecimal value,
        @JsonProperty("min_amount") BigDecimal minAmount,
        @JsonProperty("max_discount") BigDecimal maxDiscount,
        @JsonProperty("usage_limit") Integer usageLimit,
        @JsonProperty("per_user_limit") Integer perUserLimit,
        @NotEmpty @JsonProperty("scope_ref_ids") List<Long> scopeRefIds,
        @JsonProperty("starts_at") String startsAt,
        @JsonProperty("ends_at") String endsAt,
        @JsonProperty("is_active") Boolean isActive) {}
