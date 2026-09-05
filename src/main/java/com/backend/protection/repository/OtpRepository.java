package com.backend.protection.repository;

import com.backend.protection.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, String> {
    List<OtpCode> findByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(String userId, String purpose);

    @Modifying
    @Transactional
    @Query("UPDATE OtpCode o SET o.isUsed = true WHERE o.userId = :userId AND o.purpose = :purpose AND o.isUsed = false")
    void invalidateAllOtpForUser(@Param("userId") String userId, @Param("purpose") String purpose);
}
