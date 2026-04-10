package com.dujiao.api.repository;

import com.dujiao.api.domain.WalletRechargeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WalletRechargeRepository
        extends JpaRepository<WalletRechargeEntity, Long>, JpaSpecificationExecutor<WalletRechargeEntity> {

    Optional<WalletRechargeEntity> findByUserIdAndRechargeNo(long userId, String rechargeNo);

    Optional<WalletRechargeEntity> findByPaymentIdAndUserId(long paymentId, long userId);
}
