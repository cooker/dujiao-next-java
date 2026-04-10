package com.dujiao.api.repository;

import com.dujiao.api.domain.PaymentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findFirstByOrderIdOrderByIdDesc(long orderId);

    List<PaymentEntity> findByOrderIdOrderByIdDesc(long orderId);
}
