package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 与 Go {@code channelAffiliateTrackClickRequest} 对齐。 */
public record ChannelAffiliateTrackClickRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        @NotBlank @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("visitor_key") String visitorKey,
        @JsonProperty("landing_path") String landingPath,
        String referrer) {}
