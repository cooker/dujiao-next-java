package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentChannelUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 32) @JsonProperty("channel_type") String channelType,
        @JsonProperty("config_json") JsonNode configJson,
        @JsonProperty("is_active") Boolean active) {}
