package com.dujiao.api.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull @JsonProperty("product_id") Long productId,
        @NotNull @Min(1) Integer quantity) {}
