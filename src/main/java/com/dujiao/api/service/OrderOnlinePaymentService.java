package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.config.PaymentProperties;
import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.PaymentChannelEntity;
import com.dujiao.api.domain.PaymentEntity;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.dto.payment.CapturePaymentResponseDto;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.payment.LatestPaymentResponseDto;
import com.dujiao.api.dto.payment.UserCreatePaymentRequest;
import com.dujiao.api.repository.PaymentChannelRepository;
import com.dujiao.api.repository.PaymentRepository;
import com.dujiao.api.repository.ShopOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在线支付单创建与状态查询（未接第三方网关时生成可跳转的 {@code pay_url}；与 Go 响应结构对齐）。
 */
@Service
public class OrderOnlinePaymentService {

    private static final String STATUS_INITIATED = "initiated";
    private static final String STATUS_SUCCESS = "success";

    private final ShopOrderRepository shopOrderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentChannelRepository paymentChannelRepository;
    private final OrderWalletPaymentService orderWalletPaymentService;
    private final AffiliateCommissionService affiliateCommissionService;
    private final PaymentProperties paymentProperties;

    public OrderOnlinePaymentService(
            ShopOrderRepository shopOrderRepository,
            PaymentRepository paymentRepository,
            PaymentChannelRepository paymentChannelRepository,
            OrderWalletPaymentService orderWalletPaymentService,
            AffiliateCommissionService affiliateCommissionService,
            PaymentProperties paymentProperties) {
        this.shopOrderRepository = shopOrderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentChannelRepository = paymentChannelRepository;
        this.orderWalletPaymentService = orderWalletPaymentService;
        this.affiliateCommissionService = affiliateCommissionService;
        this.paymentProperties = paymentProperties;
    }

    @Transactional
    public CreatePaymentResponseDto createPaymentForUser(long userId, UserCreatePaymentRequest req, String clientIp) {
        ShopOrder order =
                shopOrderRepository
                        .findByOrderNoAndUserId(req.orderNo(), userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_status_invalid");
        }
        if (req.useBalance() && req.channelId() <= 0) {
            orderWalletPaymentService.payPendingOrderWithWallet(userId, req.orderNo());
            ShopOrder paid =
                    shopOrderRepository
                            .findByOrderNoAndUserId(req.orderNo(), userId)
                            .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
            return CreatePaymentResponseDto.walletOnly(scaleMoney(paid.getTotalAmount()));
        }
        if (req.channelId() <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_payment_request");
        }
        PaymentChannelEntity channel =
                paymentChannelRepository
                        .findById(req.channelId())
                        .filter(PaymentChannelEntity::isActive)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_payment_channel"));
        return createOnlinePayment(order, channel, clientIp);
    }

    /** 创建订单并支付：在已有待支付订单上发起在线支付。 */
    @Transactional
    public CreatePaymentResponseDto createOnlineForOrder(ShopOrder order, long channelId, String clientIp) {
        PaymentChannelEntity channel =
                paymentChannelRepository
                        .findById(channelId)
                        .filter(PaymentChannelEntity::isActive)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "invalid_payment_channel"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_status_invalid");
        }
        return createOnlinePayment(order, channel, clientIp);
    }

    @Transactional(readOnly = true)
    public LatestPaymentResponseDto latestForUser(long userId, String orderNo) {
        ShopOrder order =
                shopOrderRepository
                        .findByOrderNoAndUserId(orderNo, userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        return latestPending(order);
    }

    @Transactional(readOnly = true)
    public LatestPaymentResponseDto latestForGuest(ShopOrder order) {
        return latestPending(order);
    }

    @Transactional(readOnly = true)
    public CapturePaymentResponseDto captureForUser(long userId, long paymentId) {
        PaymentEntity p = requirePayment(paymentId);
        ShopOrder order =
                shopOrderRepository
                        .findById(p.getOrderId())
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (order.getUserId() == null || order.getUserId() != userId) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found");
        }
        return new CapturePaymentResponseDto(p.getId(), p.getStatus());
    }

    @Transactional(readOnly = true)
    public CapturePaymentResponseDto captureForGuest(long paymentId, ShopOrder order) {
        PaymentEntity p = requirePayment(paymentId);
        if (!p.getOrderId().equals(order.getId())) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "payment_not_found");
        }
        return new CapturePaymentResponseDto(p.getId(), p.getStatus());
    }

    /** 联调：将支付单与订单置为成功（仅当开启 {@code dujiao.payment.simulate-completion-enabled}）。 */
    @Transactional
    public void simulateCompletion(long paymentId, long userId) {
        if (!paymentProperties.isSimulateCompletionEnabled()) {
            throw new BusinessException(ResponseCodes.NOT_IMPLEMENTED, "payment_simulate_disabled");
        }
        PaymentEntity p = requirePayment(paymentId);
        ShopOrder order =
                shopOrderRepository
                        .findById(p.getOrderId())
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (order.getUserId() == null || order.getUserId() != userId) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found");
        }
        completePaymentSuccess(p, order);
    }

    private LatestPaymentResponseDto latestPending(ShopOrder order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_status_invalid");
        }
        PaymentEntity p =
                paymentRepository
                        .findFirstByOrderIdOrderByIdDesc(order.getId())
                        .filter(x -> STATUS_INITIATED.equals(x.getStatus()))
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "payment_not_found"));
        PaymentChannelEntity ch =
                paymentChannelRepository
                        .findById(p.getChannelId())
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "payment_channel_not_found"));
        return new LatestPaymentResponseDto(
                p.getId(),
                order.getOrderNo(),
                p.getChannelId(),
                ch.getName(),
                p.getProviderType(),
                p.getChannelType(),
                p.getInteractionMode(),
                nz(p.getPayUrl()),
                nz(p.getQrCode()),
                p.getExpiredAt());
    }

    private PaymentEntity requirePayment(long paymentId) {
        return paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "payment_not_found"));
    }

    private CreatePaymentResponseDto createOnlinePayment(
            ShopOrder order, PaymentChannelEntity channel, String clientIp) {
        BigDecimal amt = scaleMoney(order.getTotalAmount());
        Instant now = Instant.now();
        PaymentEntity p = new PaymentEntity();
        p.setOrderId(order.getId());
        p.setChannelId(channel.getId());
        p.setProviderType(nz(channel.getProviderType(), "manual"));
        p.setChannelType(channel.getChannelType());
        p.setInteractionMode(nz(channel.getInteractionMode(), "redirect"));
        p.setAmount(amt);
        p.setFeeRate(BigDecimal.ZERO);
        p.setFixedFee(BigDecimal.ZERO);
        p.setFeeAmount(BigDecimal.ZERO);
        p.setCurrency("CNY");
        p.setStatus(STATUS_INITIATED);
        p.setGatewayOrderNo("GW" + order.getId() + "-" + System.nanoTime());
        p.setExpiredAt(now.plus(30, ChronoUnit.MINUTES));
        p = paymentRepository.save(p);
        String payUrl = buildPayUrl(p, order, clientIp);
        p.setPayUrl(payUrl);
        if ("qr".equalsIgnoreCase(p.getInteractionMode())) {
            p.setQrCode(payUrl);
        }
        paymentRepository.save(p);
        return new CreatePaymentResponseDto(
                false,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                amt,
                p.getId(),
                channel.getId(),
                p.getProviderType(),
                p.getChannelType(),
                p.getInteractionMode(),
                p.getPayUrl(),
                p.getQrCode(),
                p.getExpiredAt(),
                channel.getName());
    }

    private void completePaymentSuccess(PaymentEntity p, ShopOrder order) {
        if (STATUS_SUCCESS.equals(p.getStatus())) {
            return;
        }
        if (!STATUS_INITIATED.equals(p.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "payment_status_invalid");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_status_invalid");
        }
        Instant n = Instant.now();
        p.setStatus(STATUS_SUCCESS);
        p.setPaidAt(n);
        p.setUpdatedAt(n);
        paymentRepository.save(p);
        order.setStatus(OrderStatus.PAID);
        shopOrderRepository.save(order);
        affiliateCommissionService.handleOrderPaid(order.getId());
    }

    private String buildPayUrl(PaymentEntity payment, ShopOrder order, @SuppressWarnings("unused") String clientIp) {
        String base = paymentProperties.getCheckoutBaseUrl();
        String on = URLEncoder.encode(order.getOrderNo(), StandardCharsets.UTF_8);
        String q = "payment_id=" + payment.getId() + "&order_no=" + on;
        if (base == null || base.isEmpty()) {
            return "/pay?" + q;
        }
        if (base.endsWith("/")) {
            return base + "pay?" + q;
        }
        return base + "/pay?" + q;
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String nz(String s, String d) {
        if (s == null || s.isBlank()) {
            return d;
        }
        return s;
    }
}
