package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code channelAffiliateApplyWithdrawRequest} 对齐。 */
public record ChannelAffiliateWithdrawRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        @NotBlank String amount,
        @NotBlank String channel,
        @NotBlank String account) {}
