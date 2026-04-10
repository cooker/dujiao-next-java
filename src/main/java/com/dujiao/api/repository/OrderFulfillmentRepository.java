package com.dujiao.api.repository;

import com.dujiao.api.domain.OrderFulfillmentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFulfillmentRepository extends JpaRepository<OrderFulfillmentEntity, Long> {

    Optional<OrderFulfillmentEntity> findByOrderId(long orderId);

    boolean existsByOrderId(long orderId);
}
