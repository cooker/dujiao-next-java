package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.OrderLine;
import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.domain.OrderStatusParser;
import com.dujiao.api.domain.PaymentChannelEntity;
import com.dujiao.api.domain.Product;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.order.CreateUserOrderAndPayRequest;
import com.dujiao.api.dto.order.CreateUserOrderRequest;
import com.dujiao.api.dto.order.OrderAndPayResponse;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.dto.order.OrderItemRequest;
import com.dujiao.api.repository.PaymentChannelRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.repository.ShopOrderRepository;
import com.dujiao.api.util.LocalizedTitleJson;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOrderService {

    private final ShopOrderRepository shopOrderRepository;
    private final ProductRepository productRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final OrderMappingService orderMappingService;
    private final OrderWalletPaymentService orderWalletPaymentService;
    private final AffiliateCommissionService affiliateCommissionService;
    private final OrderOnlinePaymentService orderOnlinePaymentService;

    public UserOrderService(
            ShopOrderRepository shopOrderRepository,
            ProductRepository productRepository,
            PaymentChannelRepository paymentChannelRepository,
            OrderMappingService orderMappingService,
            OrderWalletPaymentService orderWalletPaymentService,
            AffiliateCommissionService affiliateCommissionService,
            OrderOnlinePaymentService orderOnlinePaymentService) {
        this.shopOrderRepository = shopOrderRepository;
        this.productRepository = productRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.orderMappingService = orderMappingService;
        this.orderWalletPaymentService = orderWalletPaymentService;
        this.affiliateCommissionService = affiliateCommissionService;
        this.orderOnlinePaymentService = orderOnlinePaymentService;
    }

    /**
     * 与 Go {@code CreateOrderAndPay} 一致：未指定渠道且未使用余额时仅返回订单；{@code use_balance=true} 且未选在线渠道时走钱包全额支付；否则在线支付未接入时返回 {@code
     * payment_error}。
     */
    @Transactional
    public OrderAndPayResponse createAndPay(long userId, CreateUserOrderAndPayRequest req, String clientIp) {
        CreateUserOrderRequest base =
                new CreateUserOrderRequest(req.items(), req.affiliateCode(), req.affiliateVisitorKey());
        OrderDetailDto order = create(userId, base);
        String orderNo = order.orderNo();
        boolean useBal = Boolean.TRUE.equals(req.useBalance());
        Long channelId = req.channelId();
        if (useBal && channelId != null && channelId > 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_payment_request");
        }
        if (!useBal && (channelId == null || channelId <= 0)) {
            return OrderAndPayResponse.orderOnly(order, orderNo);
        }
        if (useBal && (channelId == null || channelId <= 0)) {
            OrderDetailDto paid = orderWalletPaymentService.payPendingOrderWithWallet(userId, orderNo);
            return OrderAndPayResponse.withPayment(
                    paid, orderNo, CreatePaymentResponseDto.walletOnly(paid.totalAmount()));
        }
        if (channelId != null && channelId > 0) {
            paymentChannelRepository
                    .findById(channelId)
                    .filter(PaymentChannelEntity::isActive)
                    .orElseThrow(
                            () -> new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_payment_channel"));
            ShopOrder persisted =
                    shopOrderRepository
                            .findByOrderNoAndUserId(orderNo, userId)
                            .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
            CreatePaymentResponseDto pay =
                    orderOnlinePaymentService.createOnlineForOrder(persisted, channelId, clientIp);
            OrderDetailDto refreshed = orderMappingService.toDetail(persisted);
            return OrderAndPayResponse.withPayment(refreshed, orderNo, pay);
        }
        return OrderAndPayResponse.orderOnly(order, orderNo);
    }

    @Transactional(readOnly = true)
    public ShopOrder requireOwnedOrder(long userId, String orderNo) {
        return shopOrderRepository
                .findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
    }

    @Transactional
    public OrderDetailDto create(long userId, CreateUserOrderRequest req) {
        ShopOrder order = new ShopOrder();
        order.setOrderNo(newOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        BigDecimal total = BigDecimal.ZERO;
        List<OrderLine> lines = new ArrayList<>();
        for (OrderItemRequest item : req.items()) {
            Product p =
                    productRepository
                            .findById(item.productId())
                            .filter(Product::isActive)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ResponseCodes.BAD_REQUEST, "invalid_product"));
            BigDecimal lineTotal =
                    p.getPriceAmount().multiply(BigDecimal.valueOf(item.quantity()));
            OrderLine ol = new OrderLine();
            ol.setOrder(order);
            ol.setProductId(p.getId());
            ol.setQuantity(item.quantity());
            ol.setUnitPrice(p.getPriceAmount());
            ol.setLineTotal(lineTotal);
            lines.add(ol);
            total = total.add(lineTotal);
        }
        order.setTotalAmount(total);
        order.setLines(lines);
        affiliateCommissionService.applySnapshotToOrder(order, userId, req.affiliateCode(), req.affiliateVisitorKey());
        shopOrderRepository.save(order);
        return orderMappingService.toDetail(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto preview(long userId, CreateUserOrderRequest req) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderDetailDto.OrderLineDto> lines = new ArrayList<>();
        for (OrderItemRequest item : req.items()) {
            Product p =
                    productRepository
                            .findById(item.productId())
                            .filter(Product::isActive)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ResponseCodes.BAD_REQUEST, "invalid_product"));
            BigDecimal lineTotal =
                    p.getPriceAmount().multiply(BigDecimal.valueOf(item.quantity()));
            lines.add(
                    new OrderDetailDto.OrderLineDto(
                            p.getId(),
                            LocalizedTitleJson.storedToDisplayString(p.getTitle()),
                            item.quantity(),
                            p.getPriceAmount(),
                            lineTotal));
            total = total.add(lineTotal);
        }
        return new OrderDetailDto(
                0L,
                "preview",
                null,
                userId,
                OrderStatus.PENDING,
                total,
                Instant.now(),
                lines);
    }

    @Transactional
    public void cancel(long userId, String orderNo) {
        ShopOrder o =
                shopOrderRepository
                        .findByOrderNoAndUserId(orderNo, userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (o.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_not_cancellable");
        }
        o.setStatus(OrderStatus.CANCELLED);
        shopOrderRepository.save(o);
    }

    /**
     * 与 Go {@code ListOrdersByUser} 一致：支持 {@code status}、{@code order_no}（子串匹配，忽略大小写）。
     */
    @Transactional(readOnly = true)
    public PageResponse<List<OrderDetailDto>> list(
            long userId, int page, int pageSize, String status, String orderNo) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);

        boolean filterStatus = status != null && !status.isBlank();
        OrderStatus st = filterStatus ? OrderStatusParser.parseOrNull(status) : null;
        if (filterStatus && st == null) {
            return emptyOrderPage(p, ps);
        }

        String on = orderNo == null ? "" : orderNo.trim();
        Page<ShopOrder> result;
        if (on.isEmpty() && !filterStatus) {
            result = shopOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        } else if (on.isEmpty()) {
            result =
                    shopOrderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                            userId, st, pr);
        } else if (!filterStatus) {
            result =
                    shopOrderRepository.findByUserIdAndOrderNoContainingIgnoreCaseOrderByCreatedAtDesc(
                            userId, on, pr);
        } else {
            result =
                    shopOrderRepository.findByUserIdAndStatusAndOrderNoContainingIgnoreCaseOrderByCreatedAtDesc(
                            userId, st, on, pr);
        }

        List<OrderDetailDto> list =
                result.getContent().stream().map(orderMappingService::toDetail).toList();
        PaginationDto pg =
                PaginationDto.of(
                        result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    private PageResponse<List<OrderDetailDto>> emptyOrderPage(int page, int pageSize) {
        PaginationDto pg = PaginationDto.of(page, pageSize, 0);
        return PageResponse.success(List.of(), pg);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto getByOrderNo(long userId, String orderNo) {
        ShopOrder o =
                shopOrderRepository
                        .findByOrderNoAndUserId(orderNo, userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        return orderMappingService.toDetail(o);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto getById(long userId, long orderId) {
        ShopOrder o =
                shopOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (o.getUserId() == null || o.getUserId() != userId) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found");
        }
        return orderMappingService.toDetail(o);
    }

    @Transactional
    public void cancelById(long userId, long orderId) {
        ShopOrder o =
                shopOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (o.getUserId() == null || o.getUserId() != userId) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found");
        }
        if (o.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_not_cancellable");
        }
        o.setStatus(OrderStatus.CANCELLED);
        shopOrderRepository.save(o);
    }

    private static String newOrderNo() {
        return "U"
                + System.currentTimeMillis()
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
