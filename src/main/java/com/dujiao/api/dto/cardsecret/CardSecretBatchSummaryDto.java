package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record CardSecretBatchSummaryDto(
        long id,
        @JsonProperty("product_id") long productId,
        @JsonProperty("sku_id") long skuId,
        String name,
        @JsonProperty("batch_no") String batchNo,
        String source,
        String note,
        @JsonProperty("total_count") long totalCount,
        @JsonProperty("available_count") long availableCount,
        @JsonProperty("reserved_count") long reservedCount,
        @JsonProperty("used_count") long usedCount,
        @JsonProperty("created_at") Instant createdAt) {}
