package com.dujiao.api.repository;

import com.dujiao.api.domain.AffiliateProfileEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AffiliateProfileRepository
        extends JpaRepository<AffiliateProfileEntity, Long>,
                JpaSpecificationExecutor<AffiliateProfileEntity> {

    Optional<AffiliateProfileEntity> findByUserId(long userId);

    Optional<AffiliateProfileEntity> findByAffiliateCode(String affiliateCode);

    boolean existsByAffiliateCode(String affiliateCode);

    Page<AffiliateProfileEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<AffiliateProfileEntity> findAllByOrderByIdDesc(Pageable pageable);

    List<AffiliateProfileEntity> findByUserIdIn(Collection<Long> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AffiliateProfileEntity p SET p.status = :st, p.updatedAt = :now WHERE p.id IN :ids")
    int updateStatusByIdIn(
            @Param("ids") List<Long> ids, @Param("st") String status, @Param("now") Instant now);
}
