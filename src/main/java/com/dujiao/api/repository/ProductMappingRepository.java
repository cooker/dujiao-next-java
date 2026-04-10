package com.dujiao.api.repository;

import com.dujiao.api.domain.ProductMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMappingRepository extends JpaRepository<ProductMappingEntity, Long> {}
