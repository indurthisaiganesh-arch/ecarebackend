package com.backend.protection.repository;

import com.backend.protection.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
    List<MedicalRecord> findByPatientIdOrderByRecordDateDesc(String patientId);
    List<MedicalRecord> findByDoctorIdOrderByRecordDateDesc(String doctorId);
    long countByPatientId(String patientId);
}
