package com.dujiao.api.repository;

import com.dujiao.api.domain.EmailVerificationCodeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCodeEntity, Long> {

    Optional<EmailVerificationCodeEntity> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email, String purpose);

    @Modifying
    @Query("DELETE FROM EmailVerificationCodeEntity e WHERE e.email = :email AND e.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email, @Param("purpose") String purpose);
}
