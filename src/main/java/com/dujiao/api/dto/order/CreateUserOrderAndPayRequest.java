package com.dujiao.api.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 与 Go {@code CreateOrderAndPayRequest} 对齐：未指定渠道且未使用余额时仅创建订单。
 */
public record CreateUserOrderAndPayRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        @JsonProperty("channel_id") Long channelId,
        @JsonProperty("use_balance") Boolean useBalance,
        @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("affiliate_visitor_key") String affiliateVisitorKey) {

    public CreateUserOrderAndPayRequest(List<OrderItemRequest> items, Long channelId, Boolean useBalance) {
        this(items, channelId, useBalance, null, null);
    }
}
