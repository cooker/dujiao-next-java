package com.dujiao.api.dto.cardsecret;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardSecretStatsDto(
        long total,
        long available,
        long reserved,
        @JsonProperty("used") long used) {}
