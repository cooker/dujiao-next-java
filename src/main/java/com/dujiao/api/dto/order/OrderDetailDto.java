package com.dujiao.api.dto.order;

import com.dujiao.api.domain.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record OrderDetailDto(
        long id,
        @JsonProperty("order_no") String orderNo,
        @JsonProperty("guest_email") String guestEmail,
        @JsonProperty("user_id") Long userId,
        OrderStatus status,
        @JsonProperty("total_amount") BigDecimal totalAmount,
        @JsonProperty("created_at") Instant createdAt,
        List<OrderLineDto> items,
        @JsonProperty("sub_orders") List<SubOrderDto> subOrders) {

    public OrderDetailDto(
            long id,
            String orderNo,
            String guestEmail,
            Long userId,
            OrderStatus status,
            BigDecimal totalAmount,
            Instant createdAt,
            List<OrderLineDto> items) {
        this(id, orderNo, guestEmail, userId, status, totalAmount, createdAt, items, inferSubOrders(items));
    }

    public record OrderLineDto(
            @JsonProperty("product_id") long productId,
            String title,
            int quantity,
            @JsonProperty("unit_price") BigDecimal unitPrice,
            @JsonProperty("line_total") BigDecimal lineTotal) {}

    /** 与 Go 子订单能力对齐的轻量结构：当前按订单行映射。 */
    public record SubOrderDto(
            @JsonProperty("sub_order_no") String subOrderNo,
            @JsonProperty("product_id") long productId,
            String title,
            int quantity,
            @JsonProperty("line_total") BigDecimal lineTotal) {}

    private static List<SubOrderDto> inferSubOrders(List<OrderLineDto> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<SubOrderDto> out = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            OrderLineDto line = items.get(i);
            String no = "S" + (i + 1);
            out.add(new SubOrderDto(no, line.productId(), line.title(), line.quantity(), line.lineTotal()));
        }
        return out;
    }
}
