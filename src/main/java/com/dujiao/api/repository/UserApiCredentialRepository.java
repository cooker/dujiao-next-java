package com.dujiao.api.repository;

import com.dujiao.api.domain.UserApiCredentialEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserApiCredentialRepository extends JpaRepository<UserApiCredentialEntity, Long> {

    Optional<UserApiCredentialEntity> findByUserId(long userId);

    List<UserApiCredentialEntity> findAllByOrderByIdDesc();
}
