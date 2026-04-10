package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record PaymentChannelDto(
        long id,
        String name,
        @JsonProperty("channel_type") String channelType,
        @JsonProperty("config_json") JsonNode configJson,
        @JsonProperty("is_active") boolean active) {}
