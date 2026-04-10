package com.dujiao.api.repository;

import com.dujiao.api.domain.MemberLevelPriceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLevelPriceRepository extends JpaRepository<MemberLevelPriceEntity, Long> {

    List<MemberLevelPriceEntity> findByMemberLevelId(long memberLevelId);

    Optional<MemberLevelPriceEntity> findByMemberLevelIdAndProductId(
            long memberLevelId, long productId);

    void deleteByMemberLevelId(long memberLevelId);
}
