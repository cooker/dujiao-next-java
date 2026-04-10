package com.dujiao.api.repository;

import com.dujiao.api.domain.UserLoginLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserLoginLogRepository
        extends JpaRepository<UserLoginLogEntity, Long>, JpaSpecificationExecutor<UserLoginLogEntity> {

    Page<UserLoginLogEntity> findByUserIdOrderByIdDesc(long userId, Pageable pageable);
}
