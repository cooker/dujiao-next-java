package com.dujiao.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** 与 Go {@code AffiliateDashboard} JSON 对齐。 */
public record AffiliateDashboardDto(
        boolean opened,
        @JsonProperty("affiliate_code") String affiliateCode,
        @JsonProperty("promotion_path") String promotionPath,
        @JsonProperty("click_count") long clickCount,
        @JsonProperty("valid_order_count") long validOrderCount,
        @JsonProperty("conversion_rate") double conversionRate,
        @JsonProperty("pending_commission") BigDecimal pendingCommission,
        @JsonProperty("available_commission") BigDecimal availableCommission,
        @JsonProperty("withdrawn_commission") BigDecimal withdrawnCommission) {}
