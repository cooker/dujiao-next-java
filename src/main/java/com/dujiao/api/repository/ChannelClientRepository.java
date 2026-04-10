package com.dujiao.api.repository;

import com.dujiao.api.domain.ChannelClientEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelClientRepository extends JpaRepository<ChannelClientEntity, Long> {

    Optional<ChannelClientEntity> findByClientId(String clientId);

    boolean existsByClientId(String clientId);
}
