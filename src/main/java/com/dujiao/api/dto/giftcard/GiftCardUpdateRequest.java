package com.dujiao.api.dto.giftcard;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 与 Go {@code UpdateGiftCardRequest} 对齐（字段均可缺省）。 */
public record GiftCardUpdateRequest(
        String name, String status, @JsonProperty("expires_at") String expiresAt) {}
