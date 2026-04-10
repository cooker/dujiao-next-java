package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 与 Go {@code telegramIdentityRequest} 对齐（用于 resolve/provision/open affiliate）。 */
public record ChannelTelegramIdentityRequest(
        @JsonProperty("channel_user_id") String channelUserId,
        @JsonProperty("telegram_user_id") String telegramUserId,
        String username,
        @JsonProperty("telegram_username") String telegramUsername,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("avatar_url") String avatarUrl,
        String locale) {}
