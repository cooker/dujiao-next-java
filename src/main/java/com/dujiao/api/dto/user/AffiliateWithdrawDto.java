package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record AffiliateWithdrawDto(
        long id,
        BigDecimal amount,
        String channel,
        String account,
        String status,
        @JsonProperty("reject_reason") String rejectReason,
        @JsonProperty("created_at") Instant createdAt) {}
