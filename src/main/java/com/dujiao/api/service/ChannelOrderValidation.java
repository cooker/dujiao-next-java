package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.dto.channel.ChannelOrderItemRequest;
import java.util.List;

/** 与 Go {@code buildChannelOrderItems} 校验规则一致。 */
public final class ChannelOrderValidation {

    private ChannelOrderValidation() {}

    public static void validatePreviewItems(List<ChannelOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_order_items");
        }
        for (ChannelOrderItemRequest item : items) {
            if (item.productId() <= 0 || item.quantity() <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_order_items");
            }
        }
    }

    public static void validateCreateItems(
            List<ChannelOrderItemRequest> items, Long legacyProductId, Integer legacyQuantity) {
        if (items == null || items.isEmpty()) {
            if (legacyProductId == null
                    || legacyProductId <= 0
                    || legacyQuantity == null
                    || legacyQuantity <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_order_items");
            }
            return;
        }
        for (ChannelOrderItemRequest item : items) {
            if (item.productId() <= 0 || item.quantity() <= 0) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_order_items");
            }
        }
    }
}
