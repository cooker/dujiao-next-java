package com.dujiao.api.repository;

import com.dujiao.api.domain.AuthzAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuthzAuditLogRepository
        extends JpaRepository<AuthzAuditLogEntity, Long>, JpaSpecificationExecutor<AuthzAuditLogEntity> {}
