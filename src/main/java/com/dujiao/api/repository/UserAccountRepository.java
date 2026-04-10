package com.dujiao.api.repository;

import com.dujiao.api.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByMemberLevelId(Long memberLevelId);

    List<UserAccount> findByMemberLevelIdIsNull();
}
