package com.dujiao.api.repository;

import com.dujiao.api.domain.MediaAsset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MediaAssetRepository
        extends JpaRepository<MediaAsset, Long>, JpaSpecificationExecutor<MediaAsset> {

    List<MediaAsset> findAllByOrderByIdDesc();
}
