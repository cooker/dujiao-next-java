package com.dujiao.api.repository;

import com.dujiao.api.domain.BannerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<BannerEntity, Long> {

    /** 与 Go {@code ListValidByPosition} 排序一致：{@code sort_order DESC, created_at DESC}。 */
    List<BannerEntity> findByActiveTrueOrderBySortOrderDescCreatedAtDesc();
}
