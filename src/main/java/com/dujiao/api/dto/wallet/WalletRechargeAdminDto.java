package com.dujiao.api.dto.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletRechargeAdminDto(
        long id,
        @JsonProperty("recharge_no") String rechargeNo,
        @JsonProperty("user_id") long userId,
        BigDecimal amount,
        String status,
        @JsonProperty("created_at") Instant createdAt) {}
