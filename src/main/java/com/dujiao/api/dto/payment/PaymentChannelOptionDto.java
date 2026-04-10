package com.dujiao.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentChannelOptionDto(
        long id, String name, @JsonProperty("channel_type") String channelType) {}
