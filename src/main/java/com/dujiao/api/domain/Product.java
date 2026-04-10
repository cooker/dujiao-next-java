package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "seo_meta", columnDefinition = "text")
    private String seoMetaJson;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String descriptionJson;

    @Column(columnDefinition = "text")
    private String contentJson;

    @Column(name = "price_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal priceAmount;

    /** 与 Go 一致默认 0；避免未赋值时违反库表非空约束。 */
    @Column(name = "cost_price_amount", precision = 20, scale = 2)
    private BigDecimal costPriceAmount = BigDecimal.ZERO;

    @Column(name = "images", columnDefinition = "text")
    private String imagesJson;

    @Column(columnDefinition = "text")
    private String tagsJson;

    @Column(name = "purchase_type", nullable = false, length = 20)
    private String purchaseType = "member";

    @Column(name = "max_purchase_quantity", nullable = false)
    private int maxPurchaseQuantity;

    /** 与 Go 商品默认交付类型一致；避免未显式赋值时插入 NULL 违反非空约束。 */
    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private String fulfillmentType = "manual";

    @Column(name = "manual_form_schema", columnDefinition = "text")
    private String manualFormSchemaJson;

    @Column(name = "manual_stock_total", nullable = false)
    private int manualStockTotal;

    @Column(name = "manual_stock_locked", nullable = false)
    private int manualStockLocked;

    @Column(name = "manual_stock_sold", nullable = false)
    private int manualStockSold;

    @Column(name = "payment_channel_ids", columnDefinition = "text")
    private String paymentChannelIds;

    @Column(name = "is_affiliate_enabled", nullable = false)
    private boolean affiliateEnabled;

    @Column(name = "is_mapped", nullable = false)
    private boolean mapped;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSeoMetaJson() {
        return seoMetaJson;
    }

    public void setSeoMetaJson(String seoMetaJson) {
        this.seoMetaJson = seoMetaJson;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptionJson() {
        return descriptionJson;
    }

    public void setDescriptionJson(String descriptionJson) {
        this.descriptionJson = descriptionJson;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public BigDecimal getCostPriceAmount() {
        return costPriceAmount;
    }

    public void setCostPriceAmount(BigDecimal costPriceAmount) {
        this.costPriceAmount = costPriceAmount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getFulfillmentType() {
        return fulfillmentType;
    }

    public void setFulfillmentType(String fulfillmentType) {
        this.fulfillmentType = fulfillmentType;
    }

    public int getManualStockTotal() {
        return manualStockTotal;
    }

    public void setManualStockTotal(int manualStockTotal) {
        this.manualStockTotal = manualStockTotal;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isAffiliateEnabled() {
        return affiliateEnabled;
    }

    public void setAffiliateEnabled(boolean affiliateEnabled) {
        this.affiliateEnabled = affiliateEnabled;
    }

    public String getImagesJson() {
        return imagesJson;
    }

    public void setImagesJson(String imagesJson) {
        this.imagesJson = imagesJson;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public String getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(String purchaseType) {
        this.purchaseType = purchaseType;
    }

    public int getMaxPurchaseQuantity() {
        return maxPurchaseQuantity;
    }

    public void setMaxPurchaseQuantity(int maxPurchaseQuantity) {
        this.maxPurchaseQuantity = maxPurchaseQuantity;
    }

    public String getManualFormSchemaJson() {
        return manualFormSchemaJson;
    }

    public void setManualFormSchemaJson(String manualFormSchemaJson) {
        this.manualFormSchemaJson = manualFormSchemaJson;
    }

    public int getManualStockLocked() {
        return manualStockLocked;
    }

    public void setManualStockLocked(int manualStockLocked) {
        this.manualStockLocked = manualStockLocked;
    }

    public int getManualStockSold() {
        return manualStockSold;
    }

    public void setManualStockSold(int manualStockSold) {
        this.manualStockSold = manualStockSold;
    }

    public String getPaymentChannelIds() {
        return paymentChannelIds;
    }

    public void setPaymentChannelIds(String paymentChannelIds) {
        this.paymentChannelIds = paymentChannelIds;
    }

    public boolean isMapped() {
        return mapped;
    }

    public void setMapped(boolean mapped) {
        this.mapped = mapped;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
