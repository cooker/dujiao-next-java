package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 与 Go {@code models.ProductSKU} / 表 {@code product_skus} 对齐；供 Hibernate {@code ddl-auto} 建表。
 * 业务侧 SKU 读写仍以 {@code AdminProductSkuJdbcSync} 的 JDBC 为主。
 */
@Entity
@Table(
        name = "product_skus",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "idx_product_sku_code",
                        columnNames = {"product_id", "sku_code"}))
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false, length = 64)
    private String skuCode;

    @Column(name = "spec_values", columnDefinition = "jsonb")
    private String specValuesJson;

    @Column(name = "price_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal priceAmount = BigDecimal.ZERO;

    @Column(name = "cost_price_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal costPriceAmount = BigDecimal.ZERO;

    @Column(name = "manual_stock_total", nullable = false)
    private int manualStockTotal;

    @Column(name = "manual_stock_locked", nullable = false)
    private int manualStockLocked;

    @Column(name = "manual_stock_sold", nullable = false)
    private int manualStockSold;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    /** GORM 软删；NULL 表示未删除。 */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSpecValuesJson() {
        return specValuesJson;
    }

    public void setSpecValuesJson(String specValuesJson) {
        this.specValuesJson = specValuesJson;
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

    public int getManualStockTotal() {
        return manualStockTotal;
    }

    public void setManualStockTotal(int manualStockTotal) {
        this.manualStockTotal = manualStockTotal;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
