package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 与 Go {@code AdminCreateFulfillmentRequest} 对齐。 */
public record AdminCreateFulfillmentRequest(
        @NotNull @JsonProperty("order_id") Long orderId, @NotBlank String payload) {}
