package com.dujiao.api.repository;

import com.dujiao.api.domain.UserOAuthIdentityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOAuthIdentityRepository extends JpaRepository<UserOAuthIdentityEntity, Long> {

    Optional<UserOAuthIdentityEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<UserOAuthIdentityEntity> findByUserIdAndProvider(long userId, String provider);
}
