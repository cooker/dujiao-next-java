package com.dujiao.api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/** 与 Go {@code ProductSKURequest} / {@code CreateProductRequest.skus} 一致。 */
public record ProductSkuRequest(
        Long id,
        @JsonProperty("sku_code") String skuCode,
        @JsonProperty("spec_values") JsonNode specValues,
        @JsonProperty("price_amount") BigDecimal priceAmount,
        @JsonProperty("cost_price_amount") BigDecimal costPriceAmount,
        @JsonProperty("manual_stock_total") Integer manualStockTotal,
        @JsonProperty("is_active") Boolean active,
        @JsonProperty("sort_order") Integer sortOrder) {}
