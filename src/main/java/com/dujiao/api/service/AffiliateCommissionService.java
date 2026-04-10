package com.dujiao.api.service;

import com.dujiao.api.domain.AffiliateCommissionEntity;
import com.dujiao.api.domain.AffiliateProfileEntity;
import com.dujiao.api.domain.OrderLine;
import com.dujiao.api.domain.Product;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.repository.AffiliateCommissionRepository;
import com.dujiao.api.repository.AffiliateProfileRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单侧推广佣金：下单快照、支付成功入账、取消失效、定时确认可提现（与 Go {@code AffiliateService} 核心路径对齐）。
 */
@Service
public class AffiliateCommissionService {

    private static final String PROFILE_ACTIVE = "active";
    private static final String TYPE_ORDER = "order";
    private static final String COMMISSION_PENDING = "pending_confirm";
    private static final String COMMISSION_AVAILABLE = "available";
    private static final String COMMISSION_REJECTED = "rejected";

    private final SettingsService settingsService;
    private final AffiliateProfileRepository affiliateProfileRepository;
    private final AffiliateCommissionRepository affiliateCommissionRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final ProductRepository productRepository;
    private final AffiliateClickService affiliateClickService;

    public AffiliateCommissionService(
            SettingsService settingsService,
            AffiliateProfileRepository affiliateProfileRepository,
            AffiliateCommissionRepository affiliateCommissionRepository,
            ShopOrderRepository shopOrderRepository,
            ProductRepository productRepository,
            AffiliateClickService affiliateClickService) {
        this.settingsService = settingsService;
        this.affiliateProfileRepository = affiliateProfileRepository;
        this.affiliateCommissionRepository = affiliateCommissionRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.productRepository = productRepository;
        this.affiliateClickService = affiliateClickService;
    }

    /**
     * 下单时写入 {@link ShopOrder} 的推广归因快照（与 Go {@code ResolveOrderAffiliateSnapshot} 一致：优先 30 天内
     * visitor 最近一次点击，否则 {@code affiliate_code}）。
     */
    public void applySnapshotToOrder(
            ShopOrder order, Long buyerUserId, String rawAffiliateCode, String visitorKey) {
        if (!settingsService.affiliateEnabled()) {
            return;
        }
        Optional<AffiliateProfileEntity> fromVisitor =
                affiliateClickService.resolveLatestActiveProfileForOrder(visitorKey, buyerUserId);
        if (fromVisitor.isPresent()) {
            AffiliateProfileEntity p = fromVisitor.get();
            order.setAffiliateProfileId(p.getId());
            order.setAffiliateCode(p.getAffiliateCode());
            return;
        }
        String code = normalizeAffiliateCode(rawAffiliateCode);
        if (code.isEmpty()) {
            return;
        }
        Optional<AffiliateProfileEntity> prof = affiliateProfileRepository.findByAffiliateCode(code);
        if (prof.isEmpty() || !PROFILE_ACTIVE.equals(prof.get().getStatus())) {
            return;
        }
        AffiliateProfileEntity p = prof.get();
        if (buyerUserId != null && buyerUserId > 0 && p.getUserId() == buyerUserId) {
            return;
        }
        order.setAffiliateProfileId(p.getId());
        order.setAffiliateCode(p.getAffiliateCode());
    }

    /** 与 Go {@code HandleOrderPaid}：支付成功后生成佣金行。 */
    @Transactional
    public void handleOrderPaid(long orderId) {
        if (orderId <= 0) {
            return;
        }
        if (!settingsService.affiliateEnabled()) {
            return;
        }
        BigDecimal rate = settingsService.affiliateCommissionRatePercent();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ShopOrder order = shopOrderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        AffiliateProfileEntity profile = resolveProfileForOrder(order);
        if (profile == null || !PROFILE_ACTIVE.equals(profile.getStatus())) {
            return;
        }
        if (order.getUserId() != null && order.getUserId() > 0 && profile.getUserId() == order.getUserId()) {
            return;
        }
        if (affiliateCommissionRepository.existsByAffiliateProfileIdAndOrderIdAndCommissionType(
                profile.getId(), orderId, TYPE_ORDER)) {
            return;
        }
        BigDecimal baseAmount = calculateCommissionBaseAmount(order);
        if (baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal commissionAmount =
                baseAmount.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Instant paidAt = Instant.now();
        int confirmDays = settingsService.affiliateConfirmDays();
        String status;
        Instant confirmAt = null;
        Instant availableAt = null;
        if (confirmDays <= 0) {
            status = COMMISSION_AVAILABLE;
            availableAt = paidAt;
        } else {
            status = COMMISSION_PENDING;
            confirmAt = paidAt.plus(confirmDays, ChronoUnit.DAYS);
        }
        AffiliateCommissionEntity c = new AffiliateCommissionEntity();
        c.setAffiliateProfileId(profile.getId());
        c.setOrderId(orderId);
        c.setCommissionType(TYPE_ORDER);
        c.setCommissionAmount(commissionAmount);
        c.setStatus(status);
        c.setConfirmAt(confirmAt);
        c.setAvailableAt(availableAt);
        affiliateCommissionRepository.save(c);
    }

    /** 与 Go {@code HandleOrderCanceled}：取消/退款时作废未提现佣金。 */
    @Transactional
    public void handleOrderCanceled(long orderId, String reason) {
        if (orderId <= 0) {
            return;
        }
        List<AffiliateCommissionEntity> rows =
                affiliateCommissionRepository.findByOrderIdAndStatusIn(
                        orderId, List.of(COMMISSION_PENDING, COMMISSION_AVAILABLE));
        if (rows.isEmpty()) {
            return;
        }
        String reasonText = reason == null || reason.isBlank() ? "order_canceled" : reason.trim();
        Instant now = Instant.now();
        for (AffiliateCommissionEntity item : rows) {
            if (item.getWithdrawRequestId() != null) {
                continue;
            }
            item.setStatus(COMMISSION_REJECTED);
            item.setInvalidReason(reasonText);
            item.setUpdatedAt(now);
            affiliateCommissionRepository.save(item);
        }
    }

    /** 与 Go {@code ConfirmDueCommissions}：由定时任务调用。 */
    @Transactional
    public void confirmDueCommissions() {
        if (!settingsService.affiliateEnabled()) {
            return;
        }
        Instant now = Instant.now();
        affiliateCommissionRepository.markPendingConfirmAvailable(
                COMMISSION_PENDING, COMMISSION_AVAILABLE, now, now);
    }

    private AffiliateProfileEntity resolveProfileForOrder(ShopOrder order) {
        if (order.getAffiliateProfileId() != null && order.getAffiliateProfileId() > 0) {
            return affiliateProfileRepository.findById(order.getAffiliateProfileId()).orElse(null);
        }
        if (order.getAffiliateCode() != null && !order.getAffiliateCode().isBlank()) {
            return affiliateProfileRepository.findByAffiliateCode(order.getAffiliateCode().trim()).orElse(null);
        }
        return null;
    }

    private BigDecimal calculateCommissionBaseAmount(ShopOrder order) {
        List<OrderLine> lines = order.getLines();
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Set<Long> productIds = lines.stream().map(OrderLine::getProductId).collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> map = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            Product p = map.get(line.getProductId());
            if (p == null || !p.isAffiliateEnabled()) {
                continue;
            }
            total = total.add(line.getLineTotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeAffiliateCode(String raw) {
        if (raw == null) {
            return "";
        }
        String code = raw.trim();
        if (code.isEmpty()) {
            return "";
        }
        if (code.length() > 32) {
            return code.substring(0, 32);
        }
        return code;
    }
}
