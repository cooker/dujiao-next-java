package com.dujiao.api.dto.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record WalletDto(
        BigDecimal balance,
        @JsonProperty("currency") String currency) {}
