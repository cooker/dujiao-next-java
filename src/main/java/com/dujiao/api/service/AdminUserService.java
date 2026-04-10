package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletTransaction;
import com.dujiao.api.dto.admin.AdminSetMemberLevelRequest;
import com.dujiao.api.dto.admin.AdminUserBatchStatusRequest;
import com.dujiao.api.dto.admin.AdminUserDetailDto;
import com.dujiao.api.dto.admin.AdminUserUpdateRequest;
import com.dujiao.api.dto.admin.AdminWalletAdjustRequest;
import com.dujiao.api.dto.wallet.WalletDto;
import com.dujiao.api.dto.wallet.WalletTransactionDto;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final WalletBootstrapService walletBootstrapService;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;

    public AdminUserService(
            UserAccountRepository userAccountRepository,
            MemberLevelRepository memberLevelRepository,
            WalletBootstrapService walletBootstrapService,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletService walletService) {
        this.userAccountRepository = userAccountRepository;
        this.memberLevelRepository = memberLevelRepository;
        this.walletBootstrapService = walletBootstrapService;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<AdminUserDetailDto>> list(int page, int pageSize) {
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<UserAccount> result = userAccountRepository.findAll(pr);
        List<AdminUserDetailDto> list = result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(
                        result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto get(long userId) {
        return toDto(
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found")));
    }

    @Transactional
    public Map<String, Object> batchUpdateStatus(AdminUserBatchStatusRequest req) {
        String st = req.status().trim();
        int updated = 0;
        for (Long id : req.userIds()) {
            var opt = userAccountRepository.findById(id);
            if (opt.isPresent()) {
                UserAccount u = opt.get();
                u.setStatus(st);
                userAccountRepository.save(u);
                updated++;
            }
        }
        return Map.of("updated", updated);
    }

    @Transactional
    public AdminUserDetailDto update(long userId, AdminUserUpdateRequest req) {
        UserAccount u =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (req.displayName() != null && !req.displayName().isBlank()) {
            u.setDisplayName(req.displayName().trim());
        }
        if (req.status() != null && !req.status().isBlank()) {
            u.setStatus(req.status().trim());
        }
        return toDto(userAccountRepository.save(u));
    }

    @Transactional
    public AdminUserDetailDto setMemberLevel(long userId, AdminSetMemberLevelRequest req) {
        UserAccount u =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!memberLevelRepository.existsById(req.memberLevelId())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "member_level_not_found");
        }
        u.setMemberLevelId(req.memberLevelId());
        return toDto(userAccountRepository.save(u));
    }

    @Transactional(readOnly = true)
    public WalletDto userWallet(long userId) {
        ensureUserExists(userId);
        return walletService.getWallet(userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<List<WalletTransactionDto>> userWalletTransactions(
            long userId, int page, int pageSize) {
        ensureUserExists(userId);
        return walletService.listTransactions(userId, page, pageSize);
    }

    @Transactional
    public void adjustUserWallet(long userId, AdminWalletAdjustRequest req) {
        ensureUserExists(userId);
        walletBootstrapService.ensureWallet(userId);
        WalletAccount w =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "wallet_not_found"));
        BigDecimal delta = req.amount();
        BigDecimal newBal = w.getBalance().add(delta);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "insufficient_balance");
        }
        w.setBalance(newBal);
        walletAccountRepository.save(w);
        WalletTransaction t = new WalletTransaction();
        t.setUserId(userId);
        t.setType("admin_adjust");
        t.setAmount(delta);
        t.setBalanceAfter(newBal);
        t.setRemark(req.remark() != null && !req.remark().isBlank() ? req.remark().trim() : "admin_adjust");
        walletTransactionRepository.save(t);
    }

    private void ensureUserExists(long userId) {
        if (!userAccountRepository.existsById(userId)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found");
        }
    }

    private AdminUserDetailDto toDto(UserAccount u) {
        return new AdminUserDetailDto(
                u.getId(), u.getEmail(), u.getDisplayName(), u.getStatus(), u.getMemberLevelId());
    }
}
