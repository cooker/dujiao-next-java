package com.dujiao.api.repository;

import com.dujiao.api.domain.WalletRechargeEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRechargeRepository extends JpaRepository<WalletRechargeEntity, Long> {

    Optional<WalletRechargeEntity> findByRechargeNo(String rechargeNo);

    Optional<WalletRechargeEntity> findByUserIdAndRechargeNo(long userId, String rechargeNo);

    Page<WalletRechargeEntity> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);

    Page<WalletRechargeEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
