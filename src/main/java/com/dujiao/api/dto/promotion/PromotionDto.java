package com.dujiao.api.dto.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 与 Go {@code models.Promotion} JSON 对齐。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromotionDto(
        long id,
        String name,
        @JsonProperty("scope_type") String scopeType,
        @JsonProperty("scope_ref_id") long scopeRefId,
        String type,
        String value,
        @JsonProperty("min_amount") String minAmount,
        @JsonProperty("starts_at") Instant startsAt,
        @JsonProperty("ends_at") Instant endsAt,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {}
