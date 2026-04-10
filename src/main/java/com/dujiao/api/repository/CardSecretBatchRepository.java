package com.dujiao.api.repository;

import com.dujiao.api.domain.CardSecretBatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardSecretBatchRepository extends JpaRepository<CardSecretBatchEntity, Long> {

    @Query(
            """
            SELECT b FROM CardSecretBatchEntity b
            WHERE b.productId = :productId
            AND (:skuId = 0L OR b.skuId = :skuId)
            """)
    Page<CardSecretBatchEntity> pageByProduct(
            @Param("productId") long productId, @Param("skuId") long skuId, Pageable pageable);
}
