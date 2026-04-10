package com.dujiao.api.domain;

public enum OrderStatus {
    PENDING,
    PAID,
    /** 与 Go {@code fulfilling} 对应。 */
    FULFILLING,
    CANCELLED,
    FULFILLED
}
