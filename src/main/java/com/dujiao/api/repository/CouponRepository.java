package com.dujiao.api.repository;

import com.dujiao.api.domain.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<CouponEntity, Long> {

    boolean existsByCode(String code);
}
