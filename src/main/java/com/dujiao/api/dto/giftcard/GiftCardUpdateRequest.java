package com.dujiao.api.dto.giftcard;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GiftCardUpdateRequest(BigDecimal balance, @Size(max = 32) String status) {}
