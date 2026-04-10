package com.dujiao.api.repository;

import com.dujiao.api.domain.GiftCardEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GiftCardRepository extends JpaRepository<GiftCardEntity, Long> {

    Optional<GiftCardEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GiftCardEntity g WHERE g.code = :code")
    Optional<GiftCardEntity> findByCodeForUpdate(@Param("code") String code);
}
