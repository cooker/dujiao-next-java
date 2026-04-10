package com.dujiao.api.repository;

import com.dujiao.api.domain.ProcurementOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementOrderRepository extends JpaRepository<ProcurementOrderEntity, Long> {}
