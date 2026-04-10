package com.dujiao.api.repository;

import com.dujiao.api.domain.AffiliateCommissionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AffiliateCommissionRepository extends JpaRepository<AffiliateCommissionEntity, Long> {

    boolean existsByAffiliateProfileIdAndOrderIdAndCommissionType(
            long affiliateProfileId, long orderId, String commissionType);

    List<AffiliateCommissionEntity> findByOrderIdAndStatusIn(long orderId, Collection<String> statuses);

    Page<AffiliateCommissionEntity> findByAffiliateProfileIdOrderByIdDesc(long affiliateProfileId, Pageable pageable);

    Page<AffiliateCommissionEntity> findByAffiliateProfileIdAndStatusOrderByIdDesc(
            long affiliateProfileId, String status, Pageable pageable);

    Page<AffiliateCommissionEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<AffiliateCommissionEntity> findAllByOrderByIdDesc(Pageable pageable);

    List<AffiliateCommissionEntity> findByAffiliateProfileIdAndStatusAndWithdrawRequestIdIsNullOrderByIdAsc(
            long affiliateProfileId, String status);

    List<AffiliateCommissionEntity> findByWithdrawRequestId(long withdrawRequestId);

    @Query(
            "SELECT COALESCE(SUM(c.commissionAmount), 0) FROM AffiliateCommissionEntity c WHERE "
                    + "c.affiliateProfileId = :pid AND c.status IN :statuses")
    BigDecimal sumAmountByProfileAndStatuses(
            @Param("pid") long affiliateProfileId, @Param("statuses") List<String> statuses);

    /** 有效订单数：有佣金记录且非 rejected（与 Go {@code CountValidOrdersByProfile} 一致）。 */
    @Query(
            "SELECT COUNT(DISTINCT c.orderId) FROM AffiliateCommissionEntity c WHERE c.affiliateProfileId = :pid "
                    + "AND c.status <> :rejected AND c.orderId IS NOT NULL")
    long countDistinctOrdersExcludingRejected(@Param("pid") long affiliateProfileId, @Param("rejected") String rejected);

    /** 与 Go {@code MarkPendingCommissionsAvailable} 一致：到期待确认 → 可提现。 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE AffiliateCommissionEntity c SET c.status = :available, c.availableAt = :now, c.updatedAt = :now "
                    + "WHERE c.status = :pending AND c.confirmAt IS NOT NULL AND c.confirmAt <= :before "
                    + "AND c.withdrawRequestId IS NULL")
    int markPendingConfirmAvailable(
            @Param("pending") String pending,
            @Param("available") String available,
            @Param("before") Instant before,
            @Param("now") Instant now);
}
