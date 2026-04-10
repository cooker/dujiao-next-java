package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelClientCreateResponse(
        ChannelClientDto client,
        @JsonProperty("client_secret") String clientSecret) {}
