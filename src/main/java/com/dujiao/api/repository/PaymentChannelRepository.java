package com.dujiao.api.repository;

import com.dujiao.api.domain.PaymentChannelEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentChannelRepository extends JpaRepository<PaymentChannelEntity, Long> {

    List<PaymentChannelEntity> findAllByOrderByIdAsc();

    List<PaymentChannelEntity> findByActiveTrueOrderByIdAsc();
}
