package com.dujiao.api.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** amount 为带符号变动额：正数为增加，负数为扣减。 */
public record AdminWalletAdjustRequest(
        @NotNull BigDecimal amount,
        @Size(max = 500) String remark) {}
