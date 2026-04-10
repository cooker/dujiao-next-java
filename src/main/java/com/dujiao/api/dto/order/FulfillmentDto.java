package com.dujiao.api.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** 管理端录入交付后的响应摘要，与 Go {@code Fulfillment} JSON 字段接近。 */
public record FulfillmentDto(
        long id,
        @JsonProperty("order_id") long orderId,
        String type,
        String status,
        String payload,
        @JsonProperty("delivered_at") Instant deliveredAt) {}
