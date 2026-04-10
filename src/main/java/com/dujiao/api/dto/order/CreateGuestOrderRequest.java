package com.dujiao.api.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateGuestOrderRequest(
        @Email @NotBlank String email,
        @NotBlank @JsonProperty("order_password") String orderPassword,
        @NotEmpty @Valid List<OrderItemRequest> items,
        @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("affiliate_visitor_key") String affiliateVisitorKey) {

    public CreateGuestOrderRequest(String email, String orderPassword, List<OrderItemRequest> items) {
        this(email, orderPassword, items, null, null);
    }
}
