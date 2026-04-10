package com.dujiao.api.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateUserOrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("affiliate_visitor_key") String affiliateVisitorKey) {

    public CreateUserOrderRequest(List<OrderItemRequest> items) {
        this(items, null, null);
    }
}
