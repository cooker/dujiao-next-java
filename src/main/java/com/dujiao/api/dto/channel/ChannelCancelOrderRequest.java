package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 与 Go {@code cancelOrderRequest} 对齐。 */
public record ChannelCancelOrderRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        String reason) {}
