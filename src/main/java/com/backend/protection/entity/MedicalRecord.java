package com.backend.protection.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "patient_id", length = 36, nullable = false)
    private String patientId;

    @Column(name = "doctor_id", length = 36, nullable = false)
    private String doctorId;

    @Column(name = "record_type", length = 50, nullable = false)
    private String recordType;

    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;

    // AES-256-GCM Encrypted Fields
    @Lob
    @Column(name = "encrypted_diagnosis", nullable = false)
    private String encryptedDiagnosis;

    @Lob
    @Column(name = "encrypted_treatment")
    private String encryptedTreatment;

    @Lob
    @Column(name = "encrypted_prescriptions")
    private String encryptedPrescriptions;

    @Lob
    @Column(name = "encrypted_clinical_notes")
    private String encryptedClinicalNotes;

    @Lob
    @Column(name = "encrypted_medical_history")
    private String encryptedMedicalHistory;

    // Encryption Metadata
    @Column(name = "encryption_iv", length = 64, nullable = false)
    private String encryptionIv;

    @Column(name = "encryption_auth_tag", length = 64, nullable = false)
    private String encryptionAuthTag;

    @Column(name = "key_version", length = 20, nullable = false)
    private String keyVersion;

    @Column(name = "record_hash", length = 64, nullable = false)
    private String recordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.recordDate == null) this.recordDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public LocalDateTime getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDateTime recordDate) { this.recordDate = recordDate; }
    public String getEncryptedDiagnosis() { return encryptedDiagnosis; }
    public void setEncryptedDiagnosis(String encryptedDiagnosis) { this.encryptedDiagnosis = encryptedDiagnosis; }
    public String getEncryptedTreatment() { return encryptedTreatment; }
    public void setEncryptedTreatment(String encryptedTreatment) { this.encryptedTreatment = encryptedTreatment; }
    public String getEncryptedPrescriptions() { return encryptedPrescriptions; }
    public void setEncryptedPrescriptions(String encryptedPrescriptions) { this.encryptedPrescriptions = encryptedPrescriptions; }
    public String getEncryptedClinicalNotes() { return encryptedClinicalNotes; }
    public void setEncryptedClinicalNotes(String encryptedClinicalNotes) { this.encryptedClinicalNotes = encryptedClinicalNotes; }
    public String getEncryptedMedicalHistory() { return encryptedMedicalHistory; }
    public void setEncryptedMedicalHistory(String encryptedMedicalHistory) { this.encryptedMedicalHistory = encryptedMedicalHistory; }
    public String getEncryptionIv() { return encryptionIv; }
    public void setEncryptionIv(String encryptionIv) { this.encryptionIv = encryptionIv; }
    public String getEncryptionAuthTag() { return encryptionAuthTag; }
    public void setEncryptionAuthTag(String encryptionAuthTag) { this.encryptionAuthTag = encryptionAuthTag; }
    public String getKeyVersion() { return keyVersion; }
    public void setKeyVersion(String keyVersion) { this.keyVersion = keyVersion; }
    public String getRecordHash() { return recordHash; }
    public void setRecordHash(String recordHash) { this.recordHash = recordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
