package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardSecretQueryRequest(
        @JsonProperty("product_id") Long productId,
        @JsonProperty("sku_id") Long skuId,
        @JsonProperty("batch_id") Long batchId,
        String status,
        String secret,
        @JsonProperty("batch_no") String batchNo) {}
