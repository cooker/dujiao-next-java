package com.dujiao.api.repository;

import com.dujiao.api.domain.AffiliateWithdrawEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliateWithdrawRepository extends JpaRepository<AffiliateWithdrawEntity, Long> {

    Page<AffiliateWithdrawEntity> findByAffiliateProfileIdOrderByIdDesc(long affiliateProfileId, Pageable pageable);

    Page<AffiliateWithdrawEntity> findByAffiliateProfileIdAndStatusOrderByIdDesc(
            long affiliateProfileId, String status, Pageable pageable);

    Page<AffiliateWithdrawEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<AffiliateWithdrawEntity> findAllByOrderByIdDesc(Pageable pageable);
}
