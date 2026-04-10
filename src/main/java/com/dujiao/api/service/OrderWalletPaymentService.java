package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletTransaction;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.repository.ShopOrderRepository;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录用户使用钱包余额支付待支付订单（全额扣款）。在线支付网关尚未接入时，这是可用的支付路径之一。
 */
@Service
public class OrderWalletPaymentService {

    private final ShopOrderRepository shopOrderRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletBootstrapService walletBootstrapService;
    private final OrderMappingService orderMappingService;
    private final AffiliateCommissionService affiliateCommissionService;

    public OrderWalletPaymentService(
            ShopOrderRepository shopOrderRepository,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletBootstrapService walletBootstrapService,
            OrderMappingService orderMappingService,
            AffiliateCommissionService affiliateCommissionService) {
        this.shopOrderRepository = shopOrderRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletBootstrapService = walletBootstrapService;
        this.orderMappingService = orderMappingService;
        this.affiliateCommissionService = affiliateCommissionService;
    }

    /**
     * 将待支付订单改为已支付（{@link OrderStatus#PAID}），扣减钱包并记账。
     */
    @Transactional
    public OrderDetailDto payPendingOrderWithWallet(long userId, String orderNo) {
        ShopOrder order =
                shopOrderRepository
                        .findByOrderNoAndUserId(orderNo, userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_status_invalid");
        }
        BigDecimal total = order.getTotalAmount();
        if (total == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_invalid");
        }
        walletBootstrapService.ensureWallet(userId);
        WalletAccount w =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "wallet_not_found"));
        if (w.getBalance().compareTo(total) < 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "wallet_insufficient_balance");
        }
        BigDecimal newBal = w.getBalance().subtract(total);
        w.setBalance(newBal);
        walletAccountRepository.save(w);
        WalletTransaction t = new WalletTransaction();
        t.setUserId(userId);
        t.setType("order_payment");
        t.setAmount(total.negate());
        t.setBalanceAfter(newBal);
        t.setRemark("order:" + orderNo);
        walletTransactionRepository.save(t);
        order.setStatus(OrderStatus.PAID);
        shopOrderRepository.save(order);
        affiliateCommissionService.handleOrderPaid(order.getId());
        return orderMappingService.toDetail(order);
    }
}
