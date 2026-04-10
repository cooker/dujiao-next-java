package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "affiliate_commissions")
public class AffiliateCommissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "affiliate_profile_id", nullable = false)
    private Long affiliateProfileId;

    /** 关联订单（与 Go 一致，用于幂等与取消时批量处理）。 */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "commission_type", nullable = false, length = 32)
    private String commissionType = "order";

    @Column(name = "commission_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "confirm_at")
    private Instant confirmAt;

    @Column(name = "available_at")
    private Instant availableAt;

    @Column(name = "withdraw_request_id")
    private Long withdrawRequestId;

    @Column(name = "invalid_reason", length = 255)
    private String invalidReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant n = Instant.now();
        if (createdAt == null) {
            createdAt = n;
        }
        updatedAt = n;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAffiliateProfileId() {
        return affiliateProfileId;
    }

    public void setAffiliateProfileId(Long affiliateProfileId) {
        this.affiliateProfileId = affiliateProfileId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCommissionType() {
        return commissionType;
    }

    public void setCommissionType(String commissionType) {
        this.commissionType = commissionType;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getConfirmAt() {
        return confirmAt;
    }

    public void setConfirmAt(Instant confirmAt) {
        this.confirmAt = confirmAt;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Long getWithdrawRequestId() {
        return withdrawRequestId;
    }

    public void setWithdrawRequestId(Long withdrawRequestId) {
        this.withdrawRequestId = withdrawRequestId;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public void setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
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
}
