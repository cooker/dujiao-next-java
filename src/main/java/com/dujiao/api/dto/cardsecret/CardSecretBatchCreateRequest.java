package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CardSecretBatchCreateRequest(
        @NotNull @JsonProperty("product_id") Long productId,
        @JsonProperty("sku_id") Long skuId,
        @NotEmpty @JsonProperty("secrets") List<String> secrets,
        @JsonProperty("batch_no") String batchNo,
        String note) {}
