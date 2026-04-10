package com.dujiao.api.dto.promotion;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 与 Go {@code CreatePromotionRequest} 对齐。 */
public record PromotionUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 32) String type,
        @NotNull @JsonProperty("scope_ref_id") Long scopeRefId,
        @NotNull BigDecimal value,
        @JsonProperty("min_amount") BigDecimal minAmount,
        @JsonProperty("starts_at") String startsAt,
        @JsonProperty("ends_at") String endsAt,
        @JsonProperty("is_active") Boolean isActive) {}
