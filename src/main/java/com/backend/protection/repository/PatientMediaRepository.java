package com.backend.protection.repository;

import com.backend.protection.entity.PatientMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientMediaRepository extends JpaRepository<PatientMedia, String> {
    List<PatientMedia> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<PatientMedia> findByUploaderIdOrderByCreatedAtDesc(String uploaderId);
    long countByPatientId(String patientId);
    long countByMediaType(String mediaType);
}
