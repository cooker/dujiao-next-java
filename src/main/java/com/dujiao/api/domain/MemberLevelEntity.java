package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "member_levels")
public class MemberLevelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "discount_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal discountRate = new BigDecimal("100");

    @Column(name = "recharge_threshold", nullable = false, precision = 20, scale = 2)
    private BigDecimal rechargeThreshold = BigDecimal.ZERO;

    @Column(name = "spend_threshold", nullable = false, precision = 20, scale = 2)
    private BigDecimal spendThreshold = BigDecimal.ZERO;

    @Column(name = "is_default", nullable = false)
    private boolean defaultLevel;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getRechargeThreshold() {
        return rechargeThreshold;
    }

    public void setRechargeThreshold(BigDecimal rechargeThreshold) {
        this.rechargeThreshold = rechargeThreshold;
    }

    public BigDecimal getSpendThreshold() {
        return spendThreshold;
    }

    public void setSpendThreshold(BigDecimal spendThreshold) {
        this.spendThreshold = spendThreshold;
    }

    public boolean isDefaultLevel() {
        return defaultLevel;
    }

    public void setDefaultLevel(boolean defaultLevel) {
        this.defaultLevel = defaultLevel;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
