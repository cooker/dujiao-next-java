package com.dujiao.api.service;

import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.repository.WalletAccountRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletBootstrapService {

    private final WalletAccountRepository walletAccountRepository;

    public WalletBootstrapService(WalletAccountRepository walletAccountRepository) {
        this.walletAccountRepository = walletAccountRepository;
    }

    @Transactional
    public void ensureWallet(long userId) {
        if (walletAccountRepository.existsById(userId)) {
            return;
        }
        WalletAccount w = new WalletAccount();
        w.setUserId(userId);
        w.setBalance(BigDecimal.ZERO);
        walletAccountRepository.save(w);
    }
}
