package com.backend.protection.repository;

import com.backend.protection.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    List<Permission> findByUserId(String userId);
    List<Permission> findByPatientId(String patientId);
    List<Permission> findByUserIdAndPatientId(String userId, String patientId);

    @Query("SELECT p FROM Permission p WHERE p.userId = :userId AND p.patientId = :patientId AND p.action = :action AND p.status = 'ACTIVE'")
    Optional<Permission> findActivePermission(@Param("userId") String userId,
                                               @Param("patientId") String patientId,
                                               @Param("action") String action);

    @Query("SELECT p FROM Permission p WHERE p.userId = :userId AND p.patientId = :patientId AND p.status = 'ACTIVE'")
    List<Permission> findActivePermissions(@Param("userId") String userId, @Param("patientId") String patientId);
}
