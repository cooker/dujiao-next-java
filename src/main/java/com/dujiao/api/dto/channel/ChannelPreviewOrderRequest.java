package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** 与 Go {@code previewOrderRequest} 对齐。 */
public record ChannelPreviewOrderRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        String username,
        @JsonProperty("telegram_username") String telegramUsername,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("avatar_url") String avatarUrl,
        String locale,
        List<ChannelOrderItemRequest> items,
        @JsonProperty("coupon_code") String couponCode,
        @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("affiliate_visitor_key") String affiliateVisitorKey,
        @JsonProperty("manual_form_data") Map<String, Object> manualFormData) {}
