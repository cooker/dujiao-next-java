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
@Table(name = "gift_cards")
public class GiftCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 32)
    private String status = "active";

    @Column(name = "redeemed_user_id")
    private Long redeemedUserId;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "wallet_txn_id")
    private Long walletTxnId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRedeemedUserId() {
        return redeemedUserId;
    }

    public void setRedeemedUserId(Long redeemedUserId) {
        this.redeemedUserId = redeemedUserId;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(Instant redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getWalletTxnId() {
        return walletTxnId;
    }

    public void setWalletTxnId(Long walletTxnId) {
        this.walletTxnId = walletTxnId;
    }
}
