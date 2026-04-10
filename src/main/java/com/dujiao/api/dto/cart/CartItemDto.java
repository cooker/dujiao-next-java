package com.dujiao.api.dto.cart;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CartItemDto(
        @JsonProperty("product_id") long productId,
        String title,
        String slug,
        int quantity,
        @JsonProperty("unit_price") BigDecimal unitPrice,
        @JsonProperty("line_total") BigDecimal lineTotal) {}
