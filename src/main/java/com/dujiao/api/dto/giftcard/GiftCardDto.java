package com.dujiao.api.dto.giftcard;

import java.math.BigDecimal;

public record GiftCardDto(long id, String code, BigDecimal balance, String status) {}
