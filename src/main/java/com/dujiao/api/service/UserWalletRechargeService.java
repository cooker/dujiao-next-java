package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.PaymentEntity;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletRechargeEntity;
import com.dujiao.api.dto.wallet.WalletRechargeCreateRequest;
import com.dujiao.api.dto.wallet.WalletRechargeItemDto;
import com.dujiao.api.dto.wallet.WalletRechargePaymentPayloadDto;
import com.dujiao.api.repository.PaymentRepository;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletRechargeRepository;
import com.dujiao.api.util.WalletRechargeApiMapper;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWalletRechargeService {

    private final WalletRechargeRepository walletRechargeRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletBootstrapService walletBootstrapService;
    private final PaymentRepository paymentRepository;
    private final UserPaymentService userPaymentService;

    public UserWalletRechargeService(
            WalletRechargeRepository walletRechargeRepository,
            WalletAccountRepository walletAccountRepository,
            WalletBootstrapService walletBootstrapService,
            PaymentRepository paymentRepository,
            UserPaymentService userPaymentService) {
        this.walletRechargeRepository = walletRechargeRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletBootstrapService = walletBootstrapService;
        this.paymentRepository = paymentRepository;
        this.userPaymentService = userPaymentService;
    }

    /**
     * 完整流程应对齐 Go {@code PaymentService.CreateWalletRechargePayment}；当前未实现 Java 侧下单，共用 Go
     * 库时请走 Go 创建充值或后续补齐。
     */
    @Transactional
    public WalletRechargePaymentPayloadDto create(long userId, WalletRechargeCreateRequest req) {
        Objects.requireNonNull(req);
        if (userId <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        throw new BusinessException(ResponseCodes.NOT_IMPLEMENTED, "not_implemented");
    }

    @Transactional(readOnly = true)
    public PageResponse<List<WalletRechargeItemDto>> list(
            long userId, int page, int pageSize, String status, String rechargeNo) {
        walletBootstrapService.ensureWallet(userId);
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        PageRequest pr =
                PageRequest.of(p - 1, ps, Sort.by(Sort.Direction.DESC, "id"));
        Specification<WalletRechargeEntity> spec = userRechargeSpec(userId, status, rechargeNo);
        Page<WalletRechargeEntity> result = walletRechargeRepository.findAll(spec, pr);
        List<WalletRechargeItemDto> list =
                result.getContent().stream().map(WalletRechargeApiMapper::toItem).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    private static Specification<WalletRechargeEntity> userRechargeSpec(
            long userId, String status, String rechargeNo) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("userId"), userId));
            if (status != null && !status.isBlank()) {
                preds.add(cb.equal(root.get("status"), status.trim()));
            }
            if (rechargeNo != null && !rechargeNo.isBlank()) {
                preds.add(
                        cb.like(
                                root.get("rechargeNo"),
                                "%" + rechargeNo.trim() + "%"));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public WalletRechargePaymentPayloadDto get(long userId, String rechargeNo) {
        String no = rechargeNo == null ? "" : rechargeNo.trim();
        if (no.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
        walletBootstrapService.ensureWallet(userId);
        WalletRechargeEntity r =
                walletRechargeRepository
                        .findByUserIdAndRechargeNo(userId, no)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "payment_not_found"));
        return buildPayload(userId, r);
    }

    /**
     * 与 Go {@code CaptureMyWalletRechargePayment} 一致：路径 {@code id} 为支付单 ID。钱包充值支付 {@code
     * order_id=0} 时 Java 订单捕获会失败，此处忽略 {@code order_not_found} 后返回最新快照（网关捕获待后续对接）。
     */
    @Transactional
    public WalletRechargePaymentPayloadDto capture(long userId, long paymentId) {
        WalletRechargeEntity r =
                walletRechargeRepository
                        .findByPaymentIdAndUserId(paymentId, userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "payment_not_found"));
        try {
            userPaymentService.capturePayment(userId, paymentId);
        } catch (BusinessException ex) {
            if (!"order_not_found".equals(ex.getMessage())) {
                throw ex;
            }
        }
        WalletRechargeEntity fresh =
                walletRechargeRepository
                        .findByPaymentIdAndUserId(paymentId, userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "payment_not_found"));
        return buildPayload(userId, fresh);
    }

    private WalletRechargePaymentPayloadDto buildPayload(long userId, WalletRechargeEntity r) {
        PaymentEntity payment =
                paymentRepository
                        .findById(r.getPaymentId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.INTERNAL, "payment_fetch_failed"));
        WalletAccount account =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.INTERNAL, "user_fetch_failed"));
        return WalletRechargeApiMapper.toPaymentPayload(r, payment, account);
    }
}
