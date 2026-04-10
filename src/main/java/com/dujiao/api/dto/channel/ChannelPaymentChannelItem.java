package com.dujiao.api.dto.channel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 与 Go {@code channelItem} 对齐的支付渠道摘要（实体字段不足处用占位默认值）。
 */
public record ChannelPaymentChannelItem(
        long id,
        String name,
        @JsonProperty("provider_type") String providerType,
        @JsonProperty("channel_type") String channelType,
        @JsonProperty("interaction_mode") String interactionMode,
        @JsonProperty("fee_rate") String feeRate,
        @JsonProperty("fixed_fee") String fixedFee) {}
