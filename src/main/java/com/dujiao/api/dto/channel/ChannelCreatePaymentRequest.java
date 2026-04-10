package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** 与 Go {@code createPaymentRequest} 对齐。 */
public record ChannelCreatePaymentRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        @NotNull @JsonProperty("order_id") Long orderId,
        @JsonProperty("channel_id") Long channelId,
        @JsonProperty("use_balance") Boolean useBalance) {}
