package com.dujiao.api.repository;

import com.dujiao.api.domain.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository
        extends JpaRepository<CouponEntity, Long>, JpaSpecificationExecutor<CouponEntity> {

    boolean existsByCode(String code);

    @Query(
            "SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM CouponEntity e WHERE e.code = :code AND e.id <> :excludeId")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("excludeId") long excludeId);
}
