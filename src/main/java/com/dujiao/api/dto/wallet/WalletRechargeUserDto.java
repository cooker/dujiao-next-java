package com.dujiao.api.dto.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletRechargeUserDto(
        long id,
        @JsonProperty("recharge_no") String rechargeNo,
        BigDecimal amount,
        String status,
        @JsonProperty("created_at") Instant createdAt) {}
