package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelClientSecretResetResponse(
        @JsonProperty("client_secret") String clientSecret) {}
