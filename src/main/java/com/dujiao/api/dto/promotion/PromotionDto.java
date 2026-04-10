package com.dujiao.api.dto.promotion;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PromotionDto(
        long id,
        String name,
        @JsonProperty("product_id") Long productId,
        @JsonProperty("promotion_price") BigDecimal promotionPrice,
        @JsonProperty("is_active") boolean active) {}
