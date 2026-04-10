package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** 与 Go {@code Payment} / 表 {@code payments} 对齐（核心字段）。 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType = "manual";

    @Column(name = "channel_type", nullable = false, length = 32)
    private String channelType = "alipay";

    @Column(name = "interaction_mode", nullable = false, length = 32)
    private String interactionMode = "redirect";

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "fee_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal feeRate = BigDecimal.ZERO;

    @Column(name = "fixed_fee", nullable = false, precision = 6, scale = 2)
    private BigDecimal fixedFee = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "CNY";

    @Column(nullable = false, length = 32)
    private String status = "initiated";

    @Column(name = "provider_ref", length = 128)
    private String providerRef;

    @Column(name = "gateway_order_no", length = 64)
    private String gatewayOrderNo;

    @Lob
    @Column(name = "provider_payload")
    private String providerPayload;

    @Lob
    @Column(name = "pay_url")
    private String payUrl;

    @Lob
    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "callback_at")
    private Instant callbackAt;

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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(BigDecimal feeRate) {
        this.feeRate = feeRate;
    }

    public BigDecimal getFixedFee() {
        return fixedFee;
    }

    public void setFixedFee(BigDecimal fixedFee) {
        this.fixedFee = fixedFee;
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

    public String getProviderRef() {
        return providerRef;
    }

    public void setProviderRef(String providerRef) {
        this.providerRef = providerRef;
    }

    public String getGatewayOrderNo() {
        return gatewayOrderNo;
    }

    public void setGatewayOrderNo(String gatewayOrderNo) {
        this.gatewayOrderNo = gatewayOrderNo;
    }

    public String getProviderPayload() {
        return providerPayload;
    }

    public void setProviderPayload(String providerPayload) {
        this.providerPayload = providerPayload;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
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

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getCallbackAt() {
        return callbackAt;
    }

    public void setCallbackAt(Instant callbackAt) {
        this.callbackAt = callbackAt;
    }
}
