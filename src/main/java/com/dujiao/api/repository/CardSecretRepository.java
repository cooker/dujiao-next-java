package com.dujiao.api.repository;

import com.dujiao.api.domain.CardSecretEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardSecretRepository
        extends JpaRepository<CardSecretEntity, Long>, JpaSpecificationExecutor<CardSecretEntity> {

    /** 与 Go {@code CountStockByProductIDs} 一致：按商品、SKU、卡密状态聚合。 */
    interface CardStockAggRow {
        long getProductId();

        long getSkuId();

        String getStatus();

        long getTotal();
    }

    @Query(
            """
            SELECT c.productId AS productId, c.skuId AS skuId, c.status AS status, COUNT(c) AS total
            FROM CardSecretEntity c
            WHERE c.productId IN :pids
            GROUP BY c.productId, c.skuId, c.status
            """)
    List<CardStockAggRow> countStockGroupedByProductSkuStatus(@Param("pids") Collection<Long> pids);

    @Query(
            """
            SELECT COUNT(c) FROM CardSecretEntity c
            WHERE c.productId = :pid AND (:skuId = 0L OR c.skuId = :skuId)
            """)
    long countTotalForProduct(@Param("pid") long pid, @Param("skuId") long skuId);

    @Query(
            """
            SELECT COUNT(c) FROM CardSecretEntity c
            WHERE c.productId = :pid AND (:skuId = 0L OR c.skuId = :skuId) AND c.status = :status
            """)
    long countByProductAndSkuAndStatus(
            @Param("pid") long pid, @Param("skuId") long skuId, @Param("status") String status);

    interface BatchStatusCountRow {
        long getBatchId();

        String getStatus();

        long getTotal();
    }

    @Query(
            """
            SELECT c.batch.id AS batchId, c.status AS status, COUNT(c) AS total
            FROM CardSecretEntity c
            WHERE c.batch.id IN :batchIds
            GROUP BY c.batch.id, c.status
            """)
    List<BatchStatusCountRow> countGroupedByBatchAndStatus(@Param("batchIds") Collection<Long> batchIds);

    @Query("SELECT c.id FROM CardSecretEntity c WHERE c.batch.id = :batchId ORDER BY c.id ASC")
    List<Long> listIdsByBatchId(@Param("batchId") long batchId);

    @Modifying
    @Query(
            """
            UPDATE CardSecretEntity c SET c.status = :status, c.updatedAt = :updatedAt
            WHERE c.id IN :ids
            """)
    int batchUpdateStatus(
            @Param("ids") Collection<Long> ids,
            @Param("status") String status,
            @Param("updatedAt") java.time.Instant updatedAt);

    @Modifying
    @Query("UPDATE CardSecretEntity c SET c.deletedAt = :ts WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") Collection<Long> ids, @Param("ts") java.time.Instant ts);
}
