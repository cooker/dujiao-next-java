package com.dujiao.api.dto.giftcard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GiftCardGenerateRequest(
        @NotNull @Min(1) @Max(500) Integer count,
        @NotNull @DecimalMin("0.01") BigDecimal balance) {}
