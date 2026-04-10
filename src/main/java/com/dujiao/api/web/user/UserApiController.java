package com.dujiao.api.web.user;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.dto.cart.UpsertCartItemRequest;
import com.dujiao.api.dto.order.CreateUserOrderAndPayRequest;
import com.dujiao.api.dto.order.CreateUserOrderRequest;
import com.dujiao.api.dto.order.OrderAndPayResponse;
import com.dujiao.api.dto.order.OrderDetailDto;
import com.dujiao.api.dto.payment.CapturePaymentResponseDto;
import com.dujiao.api.dto.payment.CreatePaymentResponseDto;
import com.dujiao.api.dto.payment.LatestPaymentResponseDto;
import com.dujiao.api.dto.payment.PaymentChannelOptionDto;
import com.dujiao.api.dto.payment.UserCreatePaymentRequest;
import com.dujiao.api.dto.user.AffiliateCommissionDto;
import com.dujiao.api.dto.user.AffiliateDashboardDto;
import com.dujiao.api.dto.user.AffiliateProfileOpenDto;
import com.dujiao.api.dto.user.AffiliateWithdrawApplyRequest;
import com.dujiao.api.dto.user.AffiliateWithdrawDto;
import com.dujiao.api.dto.user.ChangeEmailRequest;
import com.dujiao.api.dto.user.ChangePasswordRequest;
import com.dujiao.api.dto.user.LoginLogDto;
import com.dujiao.api.dto.user.RedeemGiftCardRequest;
import com.dujiao.api.dto.user.SendChangeEmailCodeRequest;
import com.dujiao.api.dto.user.UpdateApiCredentialStatusRequest;
import com.dujiao.api.dto.user.TelegramBindingDto;
import com.dujiao.api.dto.user.TelegramUnbindResponse;
import com.dujiao.api.dto.user.UpdateProfileRequest;
import com.dujiao.api.dto.user.UserProfileDto;
import com.dujiao.api.dto.auth.TelegramLoginRequest;
import com.dujiao.api.dto.auth.TelegramMiniAppLoginRequest;
import com.dujiao.api.dto.wallet.WalletDto;
import com.dujiao.api.dto.wallet.WalletRechargeCreateRequest;
import com.dujiao.api.dto.wallet.WalletRechargeUserDto;
import com.dujiao.api.dto.wallet.WalletTransactionDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.CartService;
import com.dujiao.api.service.PaymentChannelQueryService;
import com.dujiao.api.service.TelegramAuthService;
import com.dujiao.api.service.UserEmailChangeService;
import com.dujiao.api.service.UserAffiliateService;
import com.dujiao.api.service.UserGiftCardService;
import com.dujiao.api.service.UserLoginLogService;
import com.dujiao.api.service.UserOrderService;
import com.dujiao.api.service.OrderFulfillmentService;
import com.dujiao.api.service.UserApiCredentialService;
import com.dujiao.api.service.UserPaymentService;
import com.dujiao.api.service.UserProfileService;
import com.dujiao.api.service.UserWalletRechargeService;
import com.dujiao.api.service.WalletService;
import com.dujiao.api.web.FulfillmentHttpResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1)
@PreAuthorize("hasRole('USER')")
public class UserApiController {

    private final UserProfileService userProfileService;
    private final UserEmailChangeService userEmailChangeService;
    private final TelegramAuthService telegramAuthService;
    private final CartService cartService;
    private final UserOrderService userOrderService;
    private final UserPaymentService userPaymentService;
    private final WalletService walletService;
    private final UserWalletRechargeService userWalletRechargeService;
    private final PaymentChannelQueryService paymentChannelQueryService;
    private final UserApiCredentialService userApiCredentialService;
    private final OrderFulfillmentService orderFulfillmentService;
    private final UserLoginLogService userLoginLogService;
    private final UserGiftCardService userGiftCardService;
    private final UserAffiliateService userAffiliateService;

    public UserApiController(
            UserProfileService userProfileService,
            UserEmailChangeService userEmailChangeService,
            TelegramAuthService telegramAuthService,
            CartService cartService,
            UserOrderService userOrderService,
            UserPaymentService userPaymentService,
            WalletService walletService,
            UserWalletRechargeService userWalletRechargeService,
            PaymentChannelQueryService paymentChannelQueryService,
            UserApiCredentialService userApiCredentialService,
            OrderFulfillmentService orderFulfillmentService,
            UserLoginLogService userLoginLogService,
            UserGiftCardService userGiftCardService,
            UserAffiliateService userAffiliateService) {
        this.userProfileService = userProfileService;
        this.userEmailChangeService = userEmailChangeService;
        this.telegramAuthService = telegramAuthService;
        this.cartService = cartService;
        this.userOrderService = userOrderService;
        this.userPaymentService = userPaymentService;
        this.walletService = walletService;
        this.userWalletRechargeService = userWalletRechargeService;
        this.paymentChannelQueryService = paymentChannelQueryService;
        this.userApiCredentialService = userApiCredentialService;
        this.orderFulfillmentService = orderFulfillmentService;
        this.userLoginLogService = userLoginLogService;
        this.userGiftCardService = userGiftCardService;
        this.userAffiliateService = userAffiliateService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> me() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getUser(uid)));
    }

    @GetMapping("/me/login-logs")
    public ResponseEntity<PageResponse<List<LoginLogDto>>> loginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(userLoginLogService.listForUser(uid, page, pageSize));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userProfileService.updateProfile(uid, req)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        long uid = SecurityUtils.requireUserId();
        userProfileService.changePassword(uid, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/me/telegram")
    public ResponseEntity<ApiResponse<TelegramBindingDto>> telegramBinding() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(telegramAuthService.getTelegramBinding(uid)));
    }

    @PostMapping("/me/telegram/bind")
    public ResponseEntity<ApiResponse<TelegramBindingDto>> bindTelegram(
            @Valid @RequestBody TelegramLoginRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(telegramAuthService.bindTelegram(uid, req)));
    }

    @PostMapping("/me/telegram/miniapp/bind")
    public ResponseEntity<ApiResponse<TelegramBindingDto>> bindTelegramMiniApp(
            @RequestBody TelegramMiniAppLoginRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(telegramAuthService.bindTelegramMiniApp(uid, req)));
    }

    @DeleteMapping("/me/telegram/unbind")
    public ResponseEntity<ApiResponse<TelegramUnbindResponse>> unbindTelegram() {
        long uid = SecurityUtils.requireUserId();
        telegramAuthService.unbindTelegram(uid);
        return ResponseEntity.ok(ApiResponse.success(new TelegramUnbindResponse(true)));
    }

    @PostMapping("/me/email/send-verify-code")
    public ResponseEntity<ApiResponse<Void>> sendChangeEmailCode(
            @Valid @RequestBody SendChangeEmailCodeRequest req) {
        long uid = SecurityUtils.requireUserId();
        userEmailChangeService.sendChangeEmailCode(uid, req.kind(), req.newEmail());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/me/email/change")
    public ResponseEntity<ApiResponse<Void>> changeEmail(@Valid @RequestBody ChangeEmailRequest req) {
        long uid = SecurityUtils.requireUserId();
        userEmailChangeService.changeEmail(uid, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/cart")
    public ResponseEntity<ApiResponse<List<com.dujiao.api.dto.cart.CartItemDto>>> cart() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(uid)));
    }

    @PostMapping("/cart/items")
    public ResponseEntity<ApiResponse<List<com.dujiao.api.dto.cart.CartItemDto>>> upsertCartItem(
            @Valid @RequestBody UpsertCartItemRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(cartService.upsert(uid, req)));
    }

    @DeleteMapping("/cart/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @PathVariable("productId") long productId) {
        long uid = SecurityUtils.requireUserId();
        cartService.deleteItem(uid, productId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDetailDto>> createOrder(
            @Valid @RequestBody CreateUserOrderRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userOrderService.create(uid, req)));
    }

    @PostMapping("/orders/create-and-pay")
    public ResponseEntity<ApiResponse<OrderAndPayResponse>> createOrderAndPay(
            HttpServletRequest http, @Valid @RequestBody CreateUserOrderAndPayRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success(userOrderService.createAndPay(uid, req, clientIp(http))));
    }

    @PostMapping("/orders/preview")
    public ResponseEntity<ApiResponse<OrderDetailDto>> previewOrder(
            @Valid @RequestBody CreateUserOrderRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userOrderService.preview(uid, req)));
    }

    @PostMapping("/order/payment-channels")
    public ResponseEntity<ApiResponse<List<PaymentChannelOptionDto>>> orderPaymentChannels() {
        SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(paymentChannelQueryService.listActive()));
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<List<OrderDetailDto>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "order_no", required = false) String orderNo) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(userOrderService.list(uid, page, pageSize, status, orderNo));
    }

    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrder(
            @PathVariable("orderNo") String orderNo) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userOrderService.getByOrderNo(uid, orderNo)));
    }

    @GetMapping("/orders/{orderNo}/fulfillment/download")
    public ResponseEntity<byte[]> downloadFulfillment(@PathVariable String orderNo) {
        long uid = SecurityUtils.requireUserId();
        byte[] body = orderFulfillmentService.downloadForUser(uid, orderNo);
        return FulfillmentHttpResponses.plaintextAttachment(orderNo, body);
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable String orderNo) {
        long uid = SecurityUtils.requireUserId();
        userOrderService.cancel(uid, orderNo);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<CreatePaymentResponseDto>> createPayment(
            HttpServletRequest http, @Valid @RequestBody UserCreatePaymentRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success(userPaymentService.createPayment(uid, req, clientIp(http))));
    }

    @PostMapping("/payments/{id}/capture")
    public ResponseEntity<ApiResponse<CapturePaymentResponseDto>> capturePayment(@PathVariable String id) {
        long uid = SecurityUtils.requireUserId();
        long paymentId;
        try {
            paymentId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "payment_invalid");
        }
        return ResponseEntity.ok(ApiResponse.success(userPaymentService.capturePayment(uid, paymentId)));
    }

    @GetMapping("/payments/latest")
    public ResponseEntity<ApiResponse<LatestPaymentResponseDto>> latestPayment(
            @RequestParam(name = "order_no") String orderNo) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userPaymentService.latestPayment(uid, orderNo)));
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String ra = req.getRemoteAddr();
        return ra == null ? "" : ra;
    }

    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<WalletDto>> wallet() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(uid)));
    }

    @GetMapping("/wallet/transactions")
    public ResponseEntity<PageResponse<List<WalletTransactionDto>>> walletTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(walletService.listTransactions(uid, page, pageSize));
    }

    @PostMapping("/wallet/payment-channels")
    public ResponseEntity<ApiResponse<List<PaymentChannelOptionDto>>> walletPaymentChannels() {
        SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(paymentChannelQueryService.listActive()));
    }

    @PostMapping("/wallet/recharge")
    public ResponseEntity<ApiResponse<WalletRechargeUserDto>> rechargeWallet(
            @Valid @RequestBody WalletRechargeCreateRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userWalletRechargeService.create(uid, req)));
    }

    @GetMapping("/wallet/recharges")
    public ResponseEntity<PageResponse<List<WalletRechargeUserDto>>> listRecharges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(userWalletRechargeService.list(uid, page, pageSize));
    }

    @GetMapping("/wallet/recharges/{rechargeNo}")
    public ResponseEntity<ApiResponse<WalletRechargeUserDto>> getRecharge(
            @PathVariable String rechargeNo) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userWalletRechargeService.get(uid, rechargeNo)));
    }

    @PostMapping("/wallet/recharge/payments/{id}/capture")
    public ResponseEntity<ApiResponse<WalletRechargeUserDto>> captureRechargePayment(
            @PathVariable String id) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success(userWalletRechargeService.capture(uid, Long.parseLong(id))));
    }

    @PostMapping("/gift-cards/redeem")
    public ResponseEntity<ApiResponse<Void>> redeemGiftCard(
            @Valid @RequestBody RedeemGiftCardRequest req) {
        long uid = SecurityUtils.requireUserId();
        userGiftCardService.redeem(uid, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/affiliate/open")
    public ResponseEntity<ApiResponse<AffiliateProfileOpenDto>> openAffiliate() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userAffiliateService.openAffiliate(uid)));
    }

    @GetMapping("/affiliate/dashboard")
    public ResponseEntity<ApiResponse<AffiliateDashboardDto>> affiliateDashboard() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userAffiliateService.getDashboard(uid)));
    }

    @GetMapping("/affiliate/commissions")
    public ResponseEntity<PageResponse<List<AffiliateCommissionDto>>> affiliateCommissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(userAffiliateService.listCommissions(uid, page, pageSize, status));
    }

    @GetMapping("/affiliate/withdraws")
    public ResponseEntity<PageResponse<List<AffiliateWithdrawDto>>> affiliateWithdraws(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "status", required = false) String status) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(userAffiliateService.listWithdraws(uid, page, pageSize, status));
    }

    @PostMapping("/affiliate/withdraws")
    public ResponseEntity<ApiResponse<AffiliateWithdrawDto>> applyAffiliateWithdraw(
            @Valid @RequestBody AffiliateWithdrawApplyRequest req) {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userAffiliateService.applyWithdraw(uid, req)));
    }

    @GetMapping("/api-credential")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apiCredential() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userApiCredentialService.getForUser(uid)));
    }

    @PostMapping("/api-credential/apply")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyApiCredential() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userApiCredentialService.apply(uid)));
    }

    @PostMapping("/api-credential/regenerate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerateApiCredential() {
        long uid = SecurityUtils.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(userApiCredentialService.regenerateSecret(uid)));
    }

    @PutMapping("/api-credential/status")
    public ResponseEntity<ApiResponse<Void>> updateApiCredentialStatus(
            @Valid @RequestBody UpdateApiCredentialStatusRequest req) {
        long uid = SecurityUtils.requireUserId();
        userApiCredentialService.setActive(uid, req.isActive());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
