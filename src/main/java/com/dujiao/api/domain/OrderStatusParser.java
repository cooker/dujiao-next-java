package com.dujiao.api.domain;

/** 订单列表 {@code status} 查询参数解析（枚举名，忽略大小写）。 */
public final class OrderStatusParser {

    private OrderStatusParser() {}

    public static OrderStatus parseOrNull(String trimmed) {
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(trimmed.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
