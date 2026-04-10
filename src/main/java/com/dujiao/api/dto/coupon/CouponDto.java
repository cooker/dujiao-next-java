package com.dujiao.api.dto.coupon;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 与 Go {@code models.Coupon} JSON 对齐：金额字段为两位小数字符串。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CouponDto(
        long id,
        String code,
        String type,
        String value,
        @JsonProperty("min_amount") String minAmount,
        @JsonProperty("max_discount") String maxDiscount,
        @JsonProperty("usage_limit") int usageLimit,
        @JsonProperty("used_count") int usedCount,
        @JsonProperty("per_user_limit") int perUserLimit,
        @JsonProperty("scope_type") String scopeType,
        @JsonProperty("scope_ref_ids") String scopeRefIds,
        @JsonProperty("starts_at") Instant startsAt,
        @JsonProperty("ends_at") Instant endsAt,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {}
