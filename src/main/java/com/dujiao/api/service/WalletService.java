package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletTransaction;
import com.dujiao.api.dto.wallet.WalletDto;
import com.dujiao.api.dto.wallet.WalletTransactionDto;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletTransactionRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final String DEFAULT_CURRENCY = "CNY";

    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletBootstrapService walletBootstrapService;

    public WalletService(
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletBootstrapService walletBootstrapService) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletBootstrapService = walletBootstrapService;
    }

    @Transactional(readOnly = true)
    public WalletDto getWallet(long userId) {
        walletBootstrapService.ensureWallet(userId);
        WalletAccount w =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "wallet_not_found"));
        return new WalletDto(w.getBalance(), DEFAULT_CURRENCY);
    }

    @Transactional(readOnly = true)
    public PageResponse<List<WalletTransactionDto>> listTransactions(
            long userId, int page, int pageSize) {
        walletBootstrapService.ensureWallet(userId);
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<WalletTransaction> result =
                walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        List<WalletTransactionDto> list = result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(
                        result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    private WalletTransactionDto toDto(WalletTransaction t) {
        return new WalletTransactionDto(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getBalanceAfter(),
                t.getRemark(),
                t.getCreatedAt());
    }
}
