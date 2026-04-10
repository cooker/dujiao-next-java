package com.dujiao.api.dto.order;

import com.dujiao.api.domain.OrderStatus;

public record AdminOrderStatusPatchRequest(OrderStatus status) {}
