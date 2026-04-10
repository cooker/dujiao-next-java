package com.dujiao.api.repository;

import com.dujiao.api.domain.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PromotionRepository
        extends JpaRepository<PromotionEntity, Long>, JpaSpecificationExecutor<PromotionEntity> {}
