package com.dujiao.api.repository;

import com.dujiao.api.domain.AffiliateClickEntity;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AffiliateClickRepository extends JpaRepository<AffiliateClickEntity, Long> {

    long countByAffiliateProfileId(long affiliateProfileId);

    boolean existsByAffiliateProfileIdAndVisitorKeyAndCreatedAtAfter(
            long affiliateProfileId, String visitorKey, Instant since);

    boolean existsByAffiliateProfileIdAndVisitorKeyAndLandingPathAndCreatedAtAfter(
            long affiliateProfileId, String visitorKey, String landingPath, Instant since);

    @Query(
            "SELECT c FROM AffiliateClickEntity c WHERE c.visitorKey = :vk AND c.createdAt >= :since ORDER BY "
                    + "c.createdAt DESC, c.id DESC")
    Page<AffiliateClickEntity> findLatestForVisitor(
            @Param("vk") String visitorKey, @Param("since") Instant since, Pageable pageable);
}
