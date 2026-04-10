package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletTransaction;
import com.dujiao.api.dto.order.AdminOrderStatusPatchRequest;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.repository.ShopOrderRepository;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminShopOrderService {

    private final ShopOrderRepository shopOrderRepository;
    private final OrderMappingService orderMappingService;
    private final WalletBootstrapService walletBootstrapService;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AffiliateCommissionService affiliateCommissionService;

    public AdminShopOrderService(
            ShopOrderRepository shopOrderRepository,
            OrderMappingService orderMappingService,
            WalletBootstrapService walletBootstrapService,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            AffiliateCommissionService affiliateCommissionService) {
        this.shopOrderRepository = shopOrderRepository;
        this.orderMappingService = orderMappingService;
        this.walletBootstrapService = walletBootstrapService;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.affiliateCommissionService = affiliateCommissionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<OrderDetailDto>> list(int page, int pageSize) {
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<ShopOrder> result = shopOrderRepository.findAll(pr);
        List<OrderDetailDto> list =
                result.getContent().stream().map(orderMappingService::toDetail).toList();
        PaginationDto pg =
                PaginationDto.of(
                        result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto get(long id) {
        ShopOrder o =
                shopOrderRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        return orderMappingService.toDetail(o);
    }

    @Transactional
    public OrderDetailDto updateStatus(long orderId, AdminOrderStatusPatchRequest req) {
        if (req.status() == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "status_required");
        }
        ShopOrder o =
                shopOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        OrderStatus old = o.getStatus();
        o.setStatus(req.status());
        shopOrderRepository.save(o);
        if (req.status() == OrderStatus.PAID && old != OrderStatus.PAID) {
            affiliateCommissionService.handleOrderPaid(o.getId());
        }
        if (req.status() == OrderStatus.CANCELLED && old != OrderStatus.CANCELLED) {
            if (old == OrderStatus.PAID
                    || old == OrderStatus.FULFILLING
                    || old == OrderStatus.FULFILLED) {
                affiliateCommissionService.handleOrderCanceled(o.getId(), "order_canceled_by_admin");
            }
        }
        return orderMappingService.toDetail(o);
    }

    @Transactional
    public OrderDetailDto refundToWallet(long orderId) {
        ShopOrder o =
                shopOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (o.getUserId() == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "guest_order_no_wallet");
        }
        if (o.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_already_cancelled");
        }
        if (o.getStatus() != OrderStatus.PAID
                && o.getStatus() != OrderStatus.FULFILLING
                && o.getStatus() != OrderStatus.FULFILLED) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_not_refundable");
        }
        long userId = o.getUserId();
        walletBootstrapService.ensureWallet(userId);
        WalletAccount w =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "wallet_not_found"));
        BigDecimal amt = o.getTotalAmount();
        BigDecimal newBal = w.getBalance().add(amt);
        w.setBalance(newBal);
        walletAccountRepository.save(w);
        WalletTransaction t = new WalletTransaction();
        t.setUserId(userId);
        t.setType("order_refund");
        t.setAmount(amt);
        t.setBalanceAfter(newBal);
        t.setRemark("refund:" + o.getOrderNo());
        walletTransactionRepository.save(t);
        o.setStatus(OrderStatus.CANCELLED);
        shopOrderRepository.save(o);
        affiliateCommissionService.handleOrderCanceled(o.getId(), "order_refunded");
        return orderMappingService.toDetail(o);
    }
}
