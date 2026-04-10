package com.dujiao.api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/**
 * 管理端与公开接口共用；公开响应与 Go {@code SKUResp} 对齐（含库存展示列）。
 */
public record ProductSkuDto(
        long id,
        @JsonProperty("product_id") long productId,
        @JsonProperty("sku_code") String skuCode,
        @JsonProperty("spec_values") JsonNode specValues,
        @JsonProperty("price_amount") BigDecimal priceAmount,
        @JsonProperty("cost_price_amount") BigDecimal costPriceAmount,
        @JsonProperty("manual_stock_total") int manualStockTotal,
        @JsonProperty("manual_stock_sold") int manualStockSold,
        @JsonProperty("auto_stock_available") long autoStockAvailable,
        @JsonProperty("upstream_stock") int upstreamStock,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("sort_order") int sortOrder) {}
