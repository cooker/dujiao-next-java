package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 与 Go {@code channel_wallet.CreateWalletRecharge} 请求体对齐。 */
public record ChannelWalletRechargeRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        @NotBlank String amount,
        @NotNull @JsonProperty("channel_id") Long channelId) {}
