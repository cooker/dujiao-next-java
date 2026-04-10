package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code RedeemGiftCardRequest}（channel 版）对齐。 */
public record ChannelGiftCardRedeemRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        @NotBlank String code) {}
