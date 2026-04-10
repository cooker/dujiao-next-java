package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 与 Go {@code channelOrderItemRequest} 对齐。 */
public record ChannelOrderItemRequest(
        @JsonProperty("product_id") long productId,
        @JsonProperty("sku_id") long skuId,
        int quantity,
        @JsonProperty("fulfillment_type") String fulfillmentType) {}
