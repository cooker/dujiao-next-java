package com.dujiao.api.repository;

import com.dujiao.api.domain.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {}
