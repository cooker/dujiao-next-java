package com.dujiao.api.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 与 Go {@code CreateProductRequest} 字段一致（管理端创建/更新共用）。
 *
 * <p>更新时：{@code images}/{@code tags}/{@code payment_channel_ids} 为 {@code null} 表示不修改原值；其余标量与 JSON
 * 字段按请求体写入（与 Go 全量赋值一致）。
 */
public record ProductUpsertRequest(
        @NotNull @JsonProperty("category_id") Long categoryId,
        @NotBlank String slug,
        @NotNull @JsonProperty("title") JsonNode title,
        @JsonProperty("seo_meta") JsonNode seoMeta,
        @JsonProperty("description") JsonNode description,
        @JsonProperty("content") JsonNode content,
        @JsonProperty("manual_form_schema") JsonNode manualFormSchema,
        @NotNull @JsonProperty("price_amount") BigDecimal priceAmount,
        @JsonProperty("cost_price_amount") BigDecimal costPriceAmount,
        @JsonProperty("images") List<String> images,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("purchase_type") String purchaseType,
        @JsonProperty("max_purchase_quantity") Integer maxPurchaseQuantity,
        @JsonProperty("fulfillment_type") String fulfillmentType,
        @JsonProperty("manual_stock_total") Integer manualStockTotal,
        @Valid @JsonProperty("skus") List<ProductSkuRequest> skus,
        @JsonProperty("payment_channel_ids") List<Long> paymentChannelIds,
        @JsonProperty("is_affiliate_enabled") Boolean affiliateEnabled,
        @JsonProperty("is_active") Boolean active,
        @JsonProperty("sort_order") Integer sortOrder) {}
