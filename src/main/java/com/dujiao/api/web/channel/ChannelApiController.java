package com.dujiao.api.web.channel;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.dto.channel.ChannelAffiliateTrackClickRequest;
import com.dujiao.api.dto.channel.ChannelAffiliateWithdrawRequest;
import com.dujiao.api.dto.channel.ChannelCancelOrderRequest;
import com.dujiao.api.dto.channel.ChannelCreateOrderRequest;
import com.dujiao.api.dto.channel.ChannelCreatePaymentRequest;
import com.dujiao.api.dto.channel.ChannelGiftCardRedeemRequest;
import com.dujiao.api.dto.channel.ChannelMeDto;
import com.dujiao.api.dto.channel.ChannelOrderItemRequest;
import com.dujiao.api.dto.channel.ChannelTelegramBindRequest;
import com.dujiao.api.dto.channel.ChannelTelegramHeartbeatRequest;
import com.dujiao.api.dto.channel.ChannelTelegramIdentityRequest;
import com.dujiao.api.dto.channel.ChannelPaymentChannelItem;
import com.dujiao.api.dto.channel.ChannelPreviewOrderRequest;
import com.dujiao.api.dto.channel.ChannelWalletRechargeRequest;
import com.dujiao.api.dto.order.CreateUserOrderRequest;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.dto.order.OrderItemRequest;
import com.dujiao.api.dto.payment.CapturePaymentResponseDto;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.payment.LatestPaymentResponseDto;
import com.dujiao.api.dto.payment.UserCreatePaymentRequest;
import com.dujiao.api.dto.product.ProductDto;
import com.dujiao.api.dto.user.AffiliateCommissionDto;
import com.dujiao.api.dto.user.AffiliateDashboardDto;
import com.dujiao.api.dto.user.AffiliateProfileOpenDto;
import com.dujiao.api.dto.user.AffiliateWithdrawApplyRequest;
import com.dujiao.api.dto.user.AffiliateWithdrawDto;
import com.dujiao.api.dto.user.RedeemGiftCardRequest;
import com.dujiao.api.dto.wallet.WalletDto;
import com.dujiao.api.dto.wallet.WalletRechargeCreateRequest;
import com.dujiao.api.dto.wallet.WalletRechargePaymentPayloadDto;
import com.dujiao.api.dto.wallet.WalletTransactionDto;
import com.dujiao.api.repository.ChannelClientRepository;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.AffiliateClickService;
import com.dujiao.api.service.ChannelOrderValidation;
import com.dujiao.api.service.ChannelTelegramBotService;
import com.dujiao.api.service.ChannelTelegramIdentityService;
import com.dujiao.api.service.PaymentChannelQueryService;
import com.dujiao.api.service.PublicService;
import com.dujiao.api.service.SettingsService;
import com.dujiao.api.service.UserAffiliateService;
import com.dujiao.api.service.UserGiftCardService;
import com.dujiao.api.service.UserOrderService;
import com.dujiao.api.service.UserPaymentService;
import com.dujiao.api.service.UserWalletRechargeService;
import com.dujiao.api.service.WalletService;
import com.dujiao.api.web.ApiPaths;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(ApiPaths.V1 + "/channel")
public class ChannelApiController {

    private final PublicService publicService;
    private final ChannelClientRepository channelClientRepository;
    private final PaymentChannelQueryService paymentChannelQueryService;
    private final SettingsService settingsService;
    private final AffiliateClickService affiliateClickService;
    private final ChannelTelegramBotService channelTelegramBotService;
    private final ChannelTelegramIdentityService channelTelegramIdentityService;
    private final UserAffiliateService userAffiliateService;
    private final UserOrderService userOrderService;
    private final UserPaymentService userPaymentService;
    private final WalletService walletService;
    private final UserWalletRechargeService userWalletRechargeService;
    private final UserGiftCardService userGiftCardService;

    public ChannelApiController(
            PublicService publicService,
            ChannelClientRepository channelClientRepository,
            PaymentChannelQueryService paymentChannelQueryService,
            SettingsService settingsService,
            AffiliateClickService affiliateClickService,
            ChannelTelegramBotService channelTelegramBotService,
            ChannelTelegramIdentityService channelTelegramIdentityService,
            UserAffiliateService userAffiliateService,
            UserOrderService userOrderService,
            UserPaymentService userPaymentService,
            WalletService walletService,
            UserWalletRechargeService userWalletRechargeService,
            UserGiftCardService userGiftCardService) {
        this.publicService = publicService;
        this.channelClientRepository = channelClientRepository;
        this.paymentChannelQueryService = paymentChannelQueryService;
        this.settingsService = settingsService;
        this.affiliateClickService = affiliateClickService;
        this.channelTelegramBotService = channelTelegramBotService;
        this.channelTelegramIdentityService = channelTelegramIdentityService;
        this.userAffiliateService = userAffiliateService;
        this.userOrderService = userOrderService;
        this.userPaymentService = userPaymentService;
        this.walletService = walletService;
        this.userWalletRechargeService = userWalletRechargeService;
        this.userGiftCardService = userGiftCardService;
    }

    @GetMapping("/telegram/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> botConfig() {
        SecurityUtils.requireChannelPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success(channelTelegramBotService.getChannelBotConfigPayload()));
    }

    @PostMapping("/telegram/heartbeat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> heartbeat(
            @RequestBody(required = false) ChannelTelegramHeartbeatRequest req) {
        SecurityUtils.requireChannelPrincipal();
        return ResponseEntity.ok(ApiResponse.success(channelTelegramBotService.reportHeartbeat(req)));
    }

    @PostMapping("/identities/telegram/resolve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveTelegram(
            @RequestBody(required = false) ChannelTelegramIdentityRequest req) {
        SecurityUtils.requireChannelPrincipal();
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        return ResponseEntity.ok(ApiResponse.success(channelTelegramIdentityService.resolve(req)));
    }

    @PostMapping("/identities/telegram/provision")
    public ResponseEntity<ApiResponse<Map<String, Object>>> provisionTelegram(
            @RequestBody(required = false) ChannelTelegramIdentityRequest req) {
        SecurityUtils.requireChannelPrincipal();
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        return ResponseEntity.ok(ApiResponse.success(channelTelegramIdentityService.provision(req)));
    }

    @PostMapping("/identities/telegram/bind")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bindTelegram(
            @RequestBody(required = false) ChannelTelegramBindRequest req) {
        SecurityUtils.requireChannelPrincipal();
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        requireChannelUserInBody(req.channelUserId(), req.telegramUserId());
        if (req.bindMode() != null && !req.bindMode().isBlank()) {
            String m = req.bindMode().trim().toLowerCase();
            if (!"email_code".equals(m)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(channelTelegramIdentityService.bindByEmailCode(req)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ChannelMeDto>> me() {
        long cid = SecurityUtils.requireChannelPrincipal().channelClientId();
        var e =
                channelClientRepository
                        .findById(cid)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "channel_client_not_found"));
        return ResponseEntity.ok(
                ApiResponse.success(
                        new ChannelMeDto(
                                e.getId(), e.getName(), e.getClientId(), e.getStatus())));
    }

    /**
     * 与 Go {@code TrackAffiliateClick} 一致：校验渠道用户 id 后写入 {@code affiliate_clicks}（10 分钟内同访客+落地路径去重）。
     */
    @PostMapping("/affiliate/click")
    public ResponseEntity<ApiResponse<Map<String, Object>>> affiliateClick(
            HttpServletRequest http, @Valid @RequestBody ChannelAffiliateTrackClickRequest req) {
        SecurityUtils.requireChannelPrincipal();
        if (channelUserIdOrLegacy(req.channelUserId(), req.telegramUserId()).isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "channel_user_id_required");
        }
        affiliateClickService.trackClick(
                req.affiliateCode(),
                req.visitorKey(),
                req.landingPath(),
                req.referrer(),
                clientIp(http),
                http.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String ra = req.getRemoteAddr();
        return ra == null ? "" : ra;
    }

    @PostMapping("/affiliate/open")
    public ResponseEntity<ApiResponse<AffiliateProfileOpenDto>> affiliateOpen(
            @RequestBody(required = false) ChannelTelegramIdentityRequest req) {
        SecurityUtils.requireChannelPrincipal();
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        return ResponseEntity.ok(ApiResponse.success(userAffiliateService.openAffiliate(uid)));
    }

    @GetMapping("/affiliate/dashboard")
    @SuppressWarnings("unused")
    public ResponseEntity<ApiResponse<Map<String, Object>>> affiliateDashboard(
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId,
            @RequestParam(required = false) String locale) {
        SecurityUtils.requireChannelPrincipal();
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        AffiliateDashboardDto d = userAffiliateService.getDashboard(uid);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("opened", d.opened());
        payload.put("affiliate_code", d.affiliateCode());
        payload.put("promotion_path", d.promotionPath());
        payload.put("click_count", d.clickCount());
        payload.put("valid_order_count", d.validOrderCount());
        payload.put("conversion_rate", d.conversionRate());
        payload.put("pending_commission", d.pendingCommission());
        payload.put("available_commission", d.availableCommission());
        payload.put("withdrawn_commission", d.withdrawnCommission());
        payload.put("min_withdraw_amount", settingsService.affiliateMinWithdrawAmount());
        payload.put("withdraw_channels", settingsService.affiliateWithdrawChannels());
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/affiliate/commissions")
    @SuppressWarnings("unused")
    public ResponseEntity<ApiResponse<Map<String, Object>>> affiliateCommissions(
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        SecurityUtils.requireChannelPrincipal();
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        PageResponse<List<AffiliateCommissionDto>> r =
                userAffiliateService.listCommissions(uid, page, pageSize, status);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", r.getData());
        payload.put("page", r.getPagination().getPage());
        payload.put("page_size", r.getPagination().getPageSize());
        payload.put("total", r.getPagination().getTotal());
        payload.put("total_pages", r.getPagination().getTotalPage());
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/affiliate/withdraws")
    @SuppressWarnings("unused")
    public ResponseEntity<ApiResponse<Map<String, Object>>> affiliateWithdraws(
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        SecurityUtils.requireChannelPrincipal();
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        PageResponse<List<AffiliateWithdrawDto>> r =
                userAffiliateService.listWithdraws(uid, page, pageSize, status);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", r.getData());
        payload.put("page", r.getPagination().getPage());
        payload.put("page_size", r.getPagination().getPageSize());
        payload.put("total", r.getPagination().getTotal());
        payload.put("total_pages", r.getPagination().getTotalPage());
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/affiliate/withdraws")
    public ResponseEntity<ApiResponse<AffiliateWithdrawDto>> applyAffiliateWithdraw(
            @Valid @RequestBody ChannelAffiliateWithdrawRequest req) {
        SecurityUtils.requireChannelPrincipal();
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        userAffiliateService.applyWithdraw(
                                uid,
                                new AffiliateWithdrawApplyRequest(
                                        req.amount().trim(), req.channel().trim(), req.account().trim()))));
    }

    @GetMapping("/catalog/categories")
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalogCategories(
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(ApiResponse.success(publicService.listChannelCategories(locale)));
    }

    @GetMapping("/catalog/products")
    public ResponseEntity<PageResponse<List<ProductDto>>> catalogProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        Page<ProductDto> p = publicService.listProducts(page, pageSize);
        PaginationDto pg = publicService.pagination(p);
        return ResponseEntity.ok(PageResponse.success(p.getContent(), pg));
    }

    @GetMapping("/catalog/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> catalogProductDetail(@PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(publicService.getProductById(id)));
    }

    @GetMapping("/member-levels")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> memberLevels() {
        return ResponseEntity.ok(ApiResponse.success(publicService.listPublicMemberLevels()));
    }

    @PostMapping("/orders/preview")
    public ResponseEntity<ApiResponse<OrderDetailDto>> previewOrder(
            @RequestBody(required = false) ChannelPreviewOrderRequest req) {
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (channelUserIdOrLegacy(req.channelUserId(), req.telegramUserId()).isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "channel_user_id_required");
        }
        ChannelOrderValidation.validatePreviewItems(req.items());
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        CreateUserOrderRequest body =
                new CreateUserOrderRequest(
                        toOrderItems(req.items()), req.affiliateCode(), req.affiliateVisitorKey());
        return ResponseEntity.ok(ApiResponse.success(userOrderService.preview(uid, body)));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDetailDto>> createOrder(
            @RequestBody(required = false) ChannelCreateOrderRequest req) {
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (channelUserIdOrLegacy(req.channelUserId(), req.telegramUserId()).isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "channel_user_id_required");
        }
        ChannelOrderValidation.validateCreateItems(req.items(), req.productId(), req.quantity());
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        List<OrderItemRequest> items = toOrderItems(req.items());
        if (items.isEmpty()) {
            items = List.of(new OrderItemRequest(req.productId(), req.quantity()));
        }
        CreateUserOrderRequest body =
                new CreateUserOrderRequest(items, req.affiliateCode(), req.affiliateVisitorKey());
        return ResponseEntity.ok(ApiResponse.success(userOrderService.create(uid, body)));
    }

    @GetMapping("/orders")
    @SuppressWarnings("unused")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "5") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String locale) {
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        PageResponse<List<OrderDetailDto>> pageResp = userOrderService.list(uid, page, pageSize, status, null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", pageResp.getData());
        payload.put("page", pageResp.getPagination().getPage());
        payload.put("page_size", pageResp.getPagination().getPageSize());
        payload.put("total", pageResp.getPagination().getTotal());
        payload.put("total_pages", pageResp.getPagination().getTotalPage());
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/orders/by-order-no/{order_no}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> orderByOrderNo(
            @PathVariable("order_no") String orderNo,
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        return ResponseEntity.ok(ApiResponse.success(userOrderService.getByOrderNo(uid, orderNo)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> orderStatus(
            @PathVariable String id,
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId) {
        long orderId;
        try {
            orderId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (orderId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        return ResponseEntity.ok(ApiResponse.success(userOrderService.getById(uid, orderId)));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelOrder(
            @PathVariable String id,
            @RequestBody(required = false) ChannelCancelOrderRequest req) {
        if (req == null) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long orderId;
        try {
            orderId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (orderId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (channelUserIdOrLegacy(req.channelUserId(), req.telegramUserId()).isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "channel_user_id_required");
        }
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        userOrderService.cancelById(uid, orderId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
    }

    /**
     * 与 Go {@code GetPaymentChannels} 对齐：{@code context=recharge} 时按 {@code wallet_config.recharge_channel_ids}
     * 过滤；排除 balance/wallet 类渠道；可选 {@code wallet_only_payment}。
     */
    @SuppressWarnings("unused")
    @GetMapping("/payment-channels")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentChannels(
            @RequestParam(required = false) String context,
            @RequestParam(name = "order_no", required = false) String orderNo) {
        return ResponseEntity.ok(ApiResponse.success(buildChannelPaymentChannelsPayload(context)));
    }

    /** 与 {@code /payment-channels} 使用同一载荷（默认等同未带 {@code context} 的渠道列表）。 */
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paymentMethods() {
        return ResponseEntity.ok(ApiResponse.success(buildChannelPaymentChannelsPayload(null)));
    }

    private Map<String, Object> buildChannelPaymentChannelsPayload(String context) {
        List<ChannelPaymentChannelItem> raw;
        if ("recharge".equalsIgnoreCase(trimOrEmpty(context))) {
            List<Long> allowed = settingsService.walletRechargeChannelIds();
            raw = paymentChannelQueryService.listChannelItems();
            if (!allowed.isEmpty()) {
                raw =
                        raw.stream()
                                .filter(ch -> allowed.contains(ch.id()))
                                .collect(Collectors.toList());
            }
        } else {
            raw = paymentChannelQueryService.listChannelItems();
        }
        List<ChannelPaymentChannelItem> items =
                raw.stream().filter(this::isPayableChannelItem).collect(Collectors.toList());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("items", items);
        if (settingsService.walletOnlyPayment()) {
            resp.put("wallet_only_payment", true);
        }
        return resp;
    }

    @GetMapping("/payments/latest")
    public ResponseEntity<ApiResponse<LatestPaymentResponseDto>> latestPayment(
            @RequestParam(name = "order_id") long orderId,
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId) {
        if (orderId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        OrderDetailDto order = userOrderService.getById(uid, orderId);
        return ResponseEntity.ok(ApiResponse.success(userPaymentService.latestPayment(uid, order.orderNo())));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<CapturePaymentResponseDto>> paymentDetail(
            @PathVariable String id,
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId) {
        long paymentId;
        try {
            paymentId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (paymentId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        return ResponseEntity.ok(ApiResponse.success(userPaymentService.capturePayment(uid, paymentId)));
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<CreatePaymentResponseDto>> createPayment(
            HttpServletRequest http, @Valid @RequestBody ChannelCreatePaymentRequest req) {
        if (req.orderId() == null || req.orderId() <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        OrderDetailDto order = userOrderService.getById(uid, req.orderId());
        long channelId = req.channelId() == null ? 0L : req.channelId();
        boolean useBalance = Boolean.TRUE.equals(req.useBalance());
        UserCreatePaymentRequest body =
                new UserCreatePaymentRequest(order.orderNo(), channelId, useBalance);
        return ResponseEntity.ok(
                ApiResponse.success(userPaymentService.createPayment(uid, body, clientIp(http))));
    }

    /**
     * 与 Go {@code GetWallet} 一致需 {@code channel_user_id} 或 {@code telegram_user_id}；身份解析与钱包未接入时返回
     * {@code channel_wallet_not_implemented}。
     */
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<WalletDto>> wallet(
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId) {
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(uid)));
    }

    @GetMapping("/wallet/transactions")
    @SuppressWarnings("unused")
    public ResponseEntity<ApiResponse<Map<String, Object>>> walletTransactions(
            @RequestParam(name = "channel_user_id", required = false) String channelUserId,
            @RequestParam(name = "telegram_user_id", required = false) String telegramUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "5") int pageSize) {
        long uid = channelTelegramIdentityService.provisionAndGetUserId(channelUserId, telegramUserId);
        PageResponse<List<WalletTransactionDto>> pageResp =
                walletService.listTransactions(uid, page, pageSize);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", pageResp.getData());
        payload.put("page", pageResp.getPagination().getPage());
        payload.put("page_size", pageResp.getPagination().getPageSize());
        payload.put("total", pageResp.getPagination().getTotal());
        payload.put("total_pages", pageResp.getPagination().getTotalPage());
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @PostMapping("/wallet/gift-card/redeem")
    public ResponseEntity<ApiResponse<Void>> redeemGiftCard(
            @Valid @RequestBody ChannelGiftCardRedeemRequest req) {
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        userGiftCardService.redeem(uid, new RedeemGiftCardRequest(req.code(), null));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/wallet/recharge")
    public ResponseEntity<ApiResponse<WalletRechargePaymentPayloadDto>> walletRecharge(
            @Valid @RequestBody ChannelWalletRechargeRequest req) {
        long uid =
                channelTelegramIdentityService.provisionAndGetUserId(
                        req.channelUserId(), req.telegramUserId());
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.amount().trim());
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "validation_error");
        }
        return ResponseEntity.ok(
                ApiResponse.success(
                        userWalletRechargeService.create(uid, new WalletRechargeCreateRequest(amount))));
    }

    private static void requireChannelUserInBody(String channelUserId, String telegramUserId) {
        if (channelUserIdOrLegacy(channelUserId, telegramUserId).isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "channel_user_id_required");
        }
    }

    private static String channelUserIdOrLegacy(String channelUserId, String telegramUserId) {
        if (channelUserId != null && !channelUserId.isBlank()) {
            return channelUserId.trim();
        }
        if (telegramUserId != null && !telegramUserId.isBlank()) {
            return telegramUserId.trim();
        }
        return "";
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** 与 Go {@code GetPaymentChannels} 中跳过 {@code balance}/{@code wallet} 渠道一致。 */
    private boolean isPayableChannelItem(ChannelPaymentChannelItem ch) {
        String t = ch.channelType() != null ? ch.channelType().toLowerCase() : "";
        String p = ch.providerType() != null ? ch.providerType().toLowerCase() : "";
        return !"balance".equals(t) && !"wallet".equals(t) && !"balance".equals(p) && !"wallet".equals(p);
    }

    private static List<OrderItemRequest> toOrderItems(List<ChannelOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(it -> new OrderItemRequest(it.productId(), it.quantity()))
                .collect(Collectors.toList());
    }
}
