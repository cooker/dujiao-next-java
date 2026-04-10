package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChannelClientCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret) {}
