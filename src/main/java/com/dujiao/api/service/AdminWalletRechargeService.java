package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.domain.WalletRechargeEntity;
import com.dujiao.api.dto.wallet.WalletRechargeAdminDto;
import com.dujiao.api.repository.WalletRechargeRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminWalletRechargeService {

    private final WalletRechargeRepository walletRechargeRepository;

    public AdminWalletRechargeService(WalletRechargeRepository walletRechargeRepository) {
        this.walletRechargeRepository = walletRechargeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<WalletRechargeAdminDto>> list(int page, int pageSize) {
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<WalletRechargeEntity> result =
                walletRechargeRepository.findAllByOrderByCreatedAtDesc(pr);
        List<WalletRechargeAdminDto> list =
                result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(
                        result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    private WalletRechargeAdminDto toDto(WalletRechargeEntity e) {
        return new WalletRechargeAdminDto(
                e.getId(),
                e.getRechargeNo(),
                e.getUserId(),
                e.getAmount(),
                e.getStatus(),
                e.getCreatedAt());
    }
}
