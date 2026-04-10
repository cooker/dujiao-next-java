package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "wallet_accounts")
public class WalletAccount {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
