package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminPaymentRecordDto(
        long id,
        @JsonProperty("order_no") String orderNo,
        BigDecimal amount,
        String status,
        @JsonProperty("channel_id") Long channelId,
        @JsonProperty("created_at") Instant createdAt) {}
