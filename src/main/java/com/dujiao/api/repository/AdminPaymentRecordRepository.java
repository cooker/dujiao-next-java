package com.dujiao.api.repository;

import com.dujiao.api.domain.AdminPaymentRecordEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminPaymentRecordRepository extends JpaRepository<AdminPaymentRecordEntity, Long> {

    Page<AdminPaymentRecordEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AdminPaymentRecordEntity> findAllByOrderByCreatedAtDesc();
}
