package com.dujiao.api.repository;

import com.dujiao.api.domain.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}
