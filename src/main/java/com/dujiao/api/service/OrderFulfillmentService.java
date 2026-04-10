package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.OrderFulfillmentEntity;
import com.dujiao.api.domain.OrderStatus;
import com.dujiao.api.domain.ShopOrder;
import com.dujiao.api.dto.admin.AdminCreateFulfillmentRequest;
import com.dujiao.api.dto.order.FulfillmentDto;
import com.dujiao.api.repository.OrderFulfillmentRepository;
import com.dujiao.api.repository.ShopOrderRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单交付：下载与人工录入，与 Go {@code respondFulfillmentDownload} / {@code FulfillmentService.CreateManual}
 * 行为对齐（Java 端暂无子订单合并，仅当前订单一条交付记录）。
 */
@Service
public class OrderFulfillmentService {

    private final OrderFulfillmentRepository orderFulfillmentRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final GuestOrderService guestOrderService;

    public OrderFulfillmentService(
            OrderFulfillmentRepository orderFulfillmentRepository,
            ShopOrderRepository shopOrderRepository,
            GuestOrderService guestOrderService) {
        this.orderFulfillmentRepository = orderFulfillmentRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.guestOrderService = guestOrderService;
    }

    @Transactional(readOnly = true)
    public byte[] downloadForUser(long userId, String orderNo) {
        ShopOrder o =
                shopOrderRepository
                        .findByOrderNoAndUserId(orderNo, userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        return payloadBytes(o);
    }

    @Transactional(readOnly = true)
    public byte[] downloadForGuest(String orderNo, String email, String orderPassword) {
        ShopOrder o = guestOrderService.requireGuestOrder(orderNo, email, orderPassword);
        return payloadBytes(o);
    }

    @Transactional(readOnly = true)
    public FulfillmentDownloadResult downloadForAdmin(long orderId) {
        ShopOrder o =
                shopOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        return new FulfillmentDownloadResult(payloadBytes(o), o.getOrderNo());
    }

    /** 管理端下载：正文 + 订单号（用于附件文件名）。 */
    public record FulfillmentDownloadResult(byte[] body, String orderNo) {}

    private byte[] payloadBytes(ShopOrder order) {
        String payload =
                orderFulfillmentRepository
                        .findByOrderId(order.getId())
                        .map(OrderFulfillmentEntity::getPayload)
                        .filter(s -> s != null && !s.isBlank())
                        .orElse("");
        if (payload.isEmpty()) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "fulfillment_not_found");
        }
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 与 Go {@code CreateManual} 一致：订单须为 {@code PAID} 或 {@code FULFILLING}；录入成功后订单置为 {@code
     * FULFILLED}；同一订单仅一条交付；payload 非空。
     */
    @Transactional
    public FulfillmentDto createManual(long adminId, AdminCreateFulfillmentRequest req) {
        if (req.orderId() == null || req.orderId() <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "fulfillment_invalid");
        }
        long orderId = req.orderId();
        String payload = req.payload() == null ? "" : req.payload().trim();
        if (payload.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "fulfillment_invalid");
        }
        ShopOrder order =
                shopOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "order_not_found"));
        if (orderFulfillmentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "fulfillment_exists");
        }
        if (!canCreateManualFulfillment(order.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "order_status_invalid");
        }
        Instant now = Instant.now();
        OrderFulfillmentEntity e = new OrderFulfillmentEntity();
        e.setOrderId(orderId);
        e.setType("manual");
        e.setStatus("delivered");
        e.setPayload(payload);
        e.setDeliveredBy(adminId);
        e.setDeliveredAt(now);
        OrderFulfillmentEntity saved = orderFulfillmentRepository.save(e);
        order.setStatus(OrderStatus.FULFILLED);
        shopOrderRepository.save(order);
        return toDto(saved);
    }

    /** 与 Go {@code CreateManual}：仅 {@code paid}、{@code fulfilling} 可录入交付。 */
    private static boolean canCreateManualFulfillment(OrderStatus status) {
        return status == OrderStatus.PAID || status == OrderStatus.FULFILLING;
    }

    private static FulfillmentDto toDto(OrderFulfillmentEntity e) {
        return new FulfillmentDto(
                e.getId(), e.getOrderId(), e.getType(), e.getStatus(), e.getPayload(), e.getDeliveredAt());
    }
}
