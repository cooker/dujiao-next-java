package com.dujiao.api.repository;

import com.dujiao.api.domain.AuthzRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthzRuleRepository extends JpaRepository<AuthzRuleEntity, Long> {
    List<AuthzRuleEntity> findByPtype(String ptype);

    List<AuthzRuleEntity> findByPtypeAndV0(String ptype, String v0);

    List<AuthzRuleEntity> findByPtypeAndV0AndV1(String ptype, String v0, String v1);

    List<AuthzRuleEntity> findByPtypeAndV0AndV1AndV2(String ptype, String v0, String v1, String v2);

    void deleteByPtypeAndV0(String ptype, String v0);

    void deleteByPtypeAndV0AndV1(String ptype, String v0, String v1);

    void deleteByPtypeAndV1(String ptype, String v1);
}
