package com.dujiao.api.dto.product;

import com.dujiao.api.dto.category.CategoryDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDto(
        long id,
        @JsonProperty("category_id") long categoryId,
        String slug,
        @JsonProperty("seo_meta") JsonNode seoMeta,
        JsonNode title,
        @JsonProperty("description") JsonNode description,
        @JsonProperty("content") JsonNode content,
        @JsonProperty("price_amount") BigDecimal priceAmount,
        @JsonProperty("cost_price_amount") BigDecimal costPriceAmount,
        @JsonProperty("images") List<String> images,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("purchase_type") String purchaseType,
        @JsonProperty("max_purchase_quantity") int maxPurchaseQuantity,
        @JsonProperty("fulfillment_type") String fulfillmentType,
        @JsonProperty("manual_form_schema") JsonNode manualFormSchema,
        @JsonProperty("manual_stock_total") int manualStockTotal,
        @JsonProperty("manual_stock_locked") int manualStockLocked,
        @JsonProperty("manual_stock_sold") int manualStockSold,
        @JsonProperty("payment_channel_ids") List<Long> paymentChannelIds,
        @JsonProperty("is_mapped") boolean mapped,
        @JsonProperty("is_active") boolean active,
        @JsonProperty("sort_order") int sortOrder,
        @JsonProperty("is_affiliate_enabled") boolean affiliateEnabled,
        @JsonProperty("category") CategoryDto category,
        @JsonProperty("skus") List<ProductSkuDto> skus,
        /** 公开接口与 Go {@code ProductResp} 对齐；管理端为 null 则不输出。 */
        @JsonProperty("manual_stock_available") Integer manualStockAvailable,
        @JsonProperty("auto_stock_available") Long autoStockAvailablePublic,
        @JsonProperty("stock_status") String stockStatus,
        @JsonProperty("is_sold_out") Boolean soldOut) {}
