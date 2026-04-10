package com.dujiao.api.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 将到期的 {@code pending_confirm} 佣金转为 {@code available}（与 Go {@code ConfirmDueCommissions} 一致）。
 */
@Component
public class AffiliateCommissionConfirmScheduler {

    private final AffiliateCommissionService affiliateCommissionService;

    public AffiliateCommissionConfirmScheduler(AffiliateCommissionService affiliateCommissionService) {
        this.affiliateCommissionService = affiliateCommissionService;
    }

    @Scheduled(fixedDelayString = "${dujiao.affiliate.confirm-scan-ms:60000}")
    public void tick() {
        affiliateCommissionService.confirmDueCommissions();
    }
}
