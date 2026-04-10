package com.dujiao.api.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WalletRechargeCreateRequest(
        @NotNull @DecimalMin(value = "0.01", message = "amount_min") BigDecimal amount) {}
