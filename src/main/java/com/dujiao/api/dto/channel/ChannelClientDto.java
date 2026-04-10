package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelClientDto(
        long id,
        String name,
        @JsonProperty("client_id") String clientId,
        String status) {}
