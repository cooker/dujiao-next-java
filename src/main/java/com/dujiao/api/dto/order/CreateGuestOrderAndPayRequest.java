package com.dujiao.api.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 与 Go {@code CreateGuestOrderAndPayRequest} 对齐；{@code channel_id} 为 0 或未传时仅创建订单。
 */
public record CreateGuestOrderAndPayRequest(
        @Email @NotBlank String email,
        @NotBlank @JsonProperty("order_password") String orderPassword,
        @NotEmpty @Valid List<OrderItemRequest> items,
        @JsonProperty("channel_id") Long channelId,
        @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("affiliate_visitor_key") String affiliateVisitorKey) {

    public CreateGuestOrderAndPayRequest(
            String email, String orderPassword, List<OrderItemRequest> items, Long channelId) {
        this(email, orderPassword, items, channelId, null, null);
    }
}
