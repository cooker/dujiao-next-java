package com.dujiao.api.dto.promotion;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PromotionUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @JsonProperty("product_id") Long productId,
        @JsonProperty("promotion_price") BigDecimal promotionPrice,
        @JsonProperty("is_active") Boolean active) {}
