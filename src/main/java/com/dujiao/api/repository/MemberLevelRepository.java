package com.dujiao.api.repository;

import com.dujiao.api.domain.MemberLevelEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLevelRepository extends JpaRepository<MemberLevelEntity, Long> {

    List<MemberLevelEntity> findByActiveTrueOrderBySortOrderAsc();

    Optional<MemberLevelEntity> findBySlug(String slug);

    Optional<MemberLevelEntity> findByDefaultLevelTrue();
}
