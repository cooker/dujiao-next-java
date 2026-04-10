package com.dujiao.api.repository;

import com.dujiao.api.domain.AffiliateProfileEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliateProfileRepository extends JpaRepository<AffiliateProfileEntity, Long> {

    Optional<AffiliateProfileEntity> findByUserId(long userId);

    Optional<AffiliateProfileEntity> findByAffiliateCode(String affiliateCode);

    boolean existsByAffiliateCode(String affiliateCode);

    Page<AffiliateProfileEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<AffiliateProfileEntity> findAllByOrderByIdDesc(Pageable pageable);

    List<AffiliateProfileEntity> findByUserIdIn(Collection<Long> userIds);
}
