package com.backend.protection.repository;

import com.backend.protection.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, String> {
    Page<SecurityEvent> findByOrderByTimestampDesc(Pageable pageable);
    Page<SecurityEvent> findByDecisionOrderByTimestampDesc(String decision, Pageable pageable);
    long countByDecision(String decision);
}
