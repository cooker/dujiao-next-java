package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/** 与 Go {@code models.WalletRechargeOrder} / 表 {@code wallet_recharge_orders} 对齐。 */
@Entity
@Table(name = "wallet_recharge_orders")
@SQLDelete(sql = "UPDATE wallet_recharge_orders SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class WalletRechargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recharge_no", nullable = false, unique = true, length = 40)
    private String rechargeNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @Column(name = "channel_type", nullable = false, length = 32)
    private String channelType;

    @Column(name = "interaction_mode", nullable = false, length = 32)
    private String interactionMode;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "payable_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Column(name = "fee_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal feeRate = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 16)
    private String currency = "CNY";

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(length = 255)
    private String remark = "";

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRechargeNo() {
        return rechargeNo;
    }

    public void setRechargeNo(String rechargeNo) {
        this.rechargeNo = rechargeNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getInteractionMode() {
        return interactionMode;
    }

    public void setInteractionMode(String interactionMode) {
        this.interactionMode = interactionMode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(BigDecimal feeRate) {
        this.feeRate = feeRate;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
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
