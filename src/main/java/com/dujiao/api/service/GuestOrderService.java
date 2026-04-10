package com.dujiao.api.service;

import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.OrderLine;
import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.OrderStatusParser;
import com.dujiao.api.domain.PaymentChannelEntity;
import com.dujiao.api.domain.Product;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.order.CreateGuestOrderAndPayRequest;
import com.dujiao.api.dto.order.CreateGuestOrderRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestOrderService {

    private final ShopOrderRepository shopOrderRepository;
    private final ProductRepository productRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderMappingService orderMappingService;
    private final AffiliateCommissionService affiliateCommissionService;
    private final OrderOnlinePaymentService orderOnlinePaymentService;

    public GuestOrderService(
            ShopOrderRepository shopOrderRepository,
            ProductRepository productRepository,
            PaymentChannelRepository paymentChannelRepository,
            PasswordEncoder passwordEncoder,
            OrderMappingService orderMappingService,
            AffiliateCommissionService affiliateCommissionService,
            OrderOnlinePaymentService orderOnlinePaymentService) {
        this.shopOrderRepository = shopOrderRepository;
        this.productRepository = productRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderMappingService = orderMappingService;
        this.affiliateCommissionService = affiliateCommissionService;
        this.orderOnlinePaymentService = orderOnlinePaymentService;
    }

    /**
     * 与 Go {@code CreateGuestOrderAndPay} 一致：未指定渠道时仅返回订单；指定渠道时创建在线支付单。
     */
    @Transactional
    public OrderAndPayResponse createAndPay(CreateGuestOrderAndPayRequest req, String clientIp) {
        CreateGuestOrderRequest base =
                new CreateGuestOrderRequest(
                        req.email(),
                        req.orderPassword(),
                        req.items(),
                        req.affiliateCode(),
                        req.affiliateVisitorKey());
        OrderDetailDto order = create(base);
        String orderNo = order.orderNo();
        Long channelId = req.channelId();
        if (channelId == null || channelId <= 0) {
            return OrderAndPayResponse.orderOnly(order, orderNo);
        }
        paymentChannelRepository
                .findById(channelId)
                .filter(PaymentChannelEntity::isActive)
                .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_payment_channel"));
        ShopOrder persisted = requireGuestOrder(orderNo, req.email(), req.orderPassword());
        CreatePaymentResponseDto pay =
                orderOnlinePaymentService.createOnlineForOrder(persisted, channelId, clientIp);
        return OrderAndPayResponse.withPayment(orderMappingService.toDetail(persisted), orderNo, pay);
    }

    @Transactional(readOnly = true)
    public ShopOrder requireGuestOrder(String orderNo, String email, String orderPassword) {
        ShopOrder o =
                shopOrderRepository
                        .findByOrderNo(orderNo)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "guest_order_not_found"));
        if (!email.trim().equalsIgnoreCase(o.getGuestEmail())
                || !passwordEncoder.matches(orderPassword, o.getOrderPasswordHash())) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "guest_order_not_found");
        }
        return o;
    }

    @Transactional
    public OrderDetailDto create(CreateGuestOrderRequest req) {
        ShopOrder order = new ShopOrder();
        order.setOrderNo(newOrderNo());
        order.setGuestEmail(req.email().trim().toLowerCase());
        order.setOrderPasswordHash(passwordEncoder.encode(req.orderPassword()));
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
                                                    ResponseCodes.BAD_REQUEST,
                                                    "invalid_product"));
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
        affiliateCommissionService.applySnapshotToOrder(order, null, req.affiliateCode(), req.affiliateVisitorKey());
        shopOrderRepository.save(order);
        return orderMappingService.toDetail(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto preview(CreateGuestOrderRequest req) {
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
                                                    ResponseCodes.BAD_REQUEST,
                                                    "invalid_product"));
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
                req.email().trim().toLowerCase(),
                null,
                OrderStatus.PENDING,
                total,
                Instant.now(),
                lines);
    }

    /**
     * 与 Go {@code ListGuestOrders} 一致：若提供 {@code order_no} 则只返回匹配的一条或空页（不抛 404）。 可选 {@code
     * status}（与登录用户订单列表一致，枚举名如 {@code PENDING}）仅在未使用 {@code order_no} 时生效。
     */
    @Transactional(readOnly = true)
    public PageResponse<List<OrderDetailDto>> list(
            String email,
            String orderPassword,
            int page,
            int pageSize,
            String orderNoFilter,
            String status) {
        String on = orderNoFilter == null ? "" : orderNoFilter.trim();
        if (!on.isEmpty()) {
            try {
                OrderDetailDto one = getByOrderNo(on, email, orderPassword);
                return PageResponse.success(List.of(one), PaginationDto.of(1, 1, 1));
            } catch (BusinessException e) {
                if (e.getStatusCode() == ResponseCodes.NOT_FOUND) {
                    return PageResponse.success(List.of(), PaginationDto.of(1, 1, 0));
                }
                throw e;
            }
        }

        List<ShopOrder> all =
                shopOrderRepository.findByGuestEmailOrderByCreatedAtDesc(
                        email.trim().toLowerCase());
        List<ShopOrder> matched =
                all.stream()
                        .filter(
                                o ->
                                        passwordEncoder.matches(
                                                orderPassword, o.getOrderPasswordHash()))
                        .toList();
        boolean filterStatus = status != null && !status.isBlank();
        OrderStatus st = filterStatus ? OrderStatusParser.parseOrNull(status) : null;
        if (filterStatus && st == null) {
            int p = Math.max(page, 1);
            int ps = pageSize <= 0 ? 20 : pageSize;
            return PageResponse.success(List.of(), PaginationDto.of(p, ps, 0));
        }
        if (filterStatus) {
            matched = matched.stream().filter(o -> o.getStatus() == st).toList();
        }
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        int from = (p - 1) * ps;
        int to = Math.min(from + ps, matched.size());
        List<OrderDetailDto> slice = new ArrayList<>();
        if (from < matched.size()) {
            for (ShopOrder o : matched.subList(from, to)) {
                slice.add(orderMappingService.toDetail(o));
            }
        }
        PaginationDto pg = PaginationDto.of(p, ps, matched.size());
        return PageResponse.success(slice, pg);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto getByOrderNo(String orderNo, String email, String orderPassword) {
        ShopOrder o =
                shopOrderRepository
                        .findByOrderNo(orderNo)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "guest_order_not_found"));
        if (!email.trim().equalsIgnoreCase(o.getGuestEmail())
                || !passwordEncoder.matches(orderPassword, o.getOrderPasswordHash())) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "guest_order_not_found");
        }
        return orderMappingService.toDetail(o);
    }

    private static String newOrderNo() {
        return "G"
                + System.currentTimeMillis()
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
