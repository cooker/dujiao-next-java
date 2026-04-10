package com.dujiao.api.dto.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionDto(
        long id,
        String type,
        BigDecimal amount,
        @JsonProperty("balance_after") BigDecimal balanceAfter,
        String remark,
        @JsonProperty("created_at") Instant createdAt) {}
