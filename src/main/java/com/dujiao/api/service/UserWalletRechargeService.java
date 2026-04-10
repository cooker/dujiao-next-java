package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletRechargeEntity;
import com.dujiao.api.domain.WalletTransaction;
import com.dujiao.api.dto.wallet.WalletRechargeCreateRequest;
import com.dujiao.api.dto.wallet.WalletRechargeUserDto;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletRechargeRepository;
import com.dujiao.api.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWalletRechargeService {

    private final WalletRechargeRepository walletRechargeRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletBootstrapService walletBootstrapService;

    public UserWalletRechargeService(
            WalletRechargeRepository walletRechargeRepository,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletBootstrapService walletBootstrapService) {
        this.walletRechargeRepository = walletRechargeRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletBootstrapService = walletBootstrapService;
    }

    @Transactional
    public WalletRechargeUserDto create(long userId, WalletRechargeCreateRequest req) {
        WalletRechargeEntity e = new WalletRechargeEntity();
        e.setUserId(userId);
        e.setRechargeNo("RCH" + UUID.randomUUID().toString().replace("-", ""));
        e.setAmount(req.amount());
        e.setStatus("pending");
        return toDto(walletRechargeRepository.save(e));
    }

    @Transactional(readOnly = true)
    public PageResponse<List<WalletRechargeUserDto>> list(long userId, int page, int pageSize) {
        walletBootstrapService.ensureWallet(userId);
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<WalletRechargeEntity> result = walletRechargeRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        List<WalletRechargeUserDto> list = result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public WalletRechargeUserDto get(long userId, String rechargeNo) {
        WalletRechargeEntity e =
                walletRechargeRepository
                        .findByUserIdAndRechargeNo(userId, rechargeNo)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "recharge_not_found"));
        return toDto(e);
    }

    /** 模拟支付网关确认入账：pending → completed，余额增加。 */
    @Transactional
    public WalletRechargeUserDto capture(long userId, long rechargeId) {
        WalletRechargeEntity r =
                walletRechargeRepository
                        .findById(rechargeId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "recharge_not_found"));
        if (r.getUserId() != userId) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "recharge_forbidden");
        }
        if (!"pending".equals(r.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "recharge_not_pending");
        }
        walletBootstrapService.ensureWallet(userId);
        WalletAccount w =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "wallet_not_found"));
        BigDecimal delta = r.getAmount();
        BigDecimal newBal = w.getBalance().add(delta);
        w.setBalance(newBal);
        walletAccountRepository.save(w);
        WalletTransaction t = new WalletTransaction();
        t.setUserId(userId);
        t.setType("wallet_recharge");
        t.setAmount(delta);
        t.setBalanceAfter(newBal);
        t.setRemark("recharge:" + r.getRechargeNo());
        walletTransactionRepository.save(t);
        r.setStatus("completed");
        walletRechargeRepository.save(r);
        return toDto(r);
    }

    private WalletRechargeUserDto toDto(WalletRechargeEntity e) {
        return new WalletRechargeUserDto(
                e.getId(), e.getRechargeNo(), e.getAmount(), e.getStatus(), e.getCreatedAt());
    }
}
