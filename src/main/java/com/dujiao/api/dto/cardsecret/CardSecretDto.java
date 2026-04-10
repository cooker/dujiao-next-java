package com.dujiao.api.dto.cardsecret;

import com.dujiao.api.domain.CardSecretEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record CardSecretDto(
        long id,
        @JsonProperty("product_id") long productId,
        @JsonProperty("sku_id") long skuId,
        @JsonProperty("batch_id") Long batchId,
        String secret,
        String status,
        @JsonProperty("order_id") Long orderId,
        @JsonProperty("reserved_at") Instant reservedAt,
        @JsonProperty("used_at") Instant usedAt,
        @JsonProperty("created_at") Instant createdAt) {

    public static CardSecretDto from(CardSecretEntity e) {
        return new CardSecretDto(
                e.getId(),
                e.getProductId(),
                e.getSkuId(),
                e.getBatchId(),
                e.getSecret(),
                e.getStatus(),
                e.getOrderId(),
                e.getReservedAt(),
                e.getUsedAt(),
                e.getCreatedAt());
    }
}
