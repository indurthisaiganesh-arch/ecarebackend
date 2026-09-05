package com.backend.protection.service;

import com.backend.protection.ai.AnomalyDetectionService;
import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.record.CreateRecordRequest;
import com.backend.protection.dto.record.MedicalRecordResponse;
import com.backend.protection.encryption.AesEncryptionService;
import com.backend.protection.entity.MedicalRecord;
import com.backend.protection.entity.Patient;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.exception.ResourceNotFoundException;
import com.backend.protection.repository.MedicalRecordRepository;
import com.backend.protection.repository.PatientRepository;
import com.backend.protection.repository.PermissionRepository;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository recordRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final AesEncryptionService encryptionService;
    private final AuditEventService auditEventService;
    private final AnomalyDetectionService anomalyDetectionService;

    // Cache doctor names to avoid redundant queries
    private final Map<String, String> doctorNameCache = new ConcurrentHashMap<>();

    public MedicalRecordService(
            MedicalRecordRepository recordRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            PermissionRepository permissionRepository,
            AesEncryptionService encryptionService,
            AuditEventService auditEventService,
            AnomalyDetectionService anomalyDetectionService) {
        this.recordRepository = recordRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.encryptionService = encryptionService;
        this.auditEventService = auditEventService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getRecordsForPatient(
            String patientId,
            UserPrincipal principal,
            HttpServletRequest request) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        verifyReadAccess(patient, principal, request);

        // Anomaly detection evaluation
        anomalyDetectionService.evaluate(principal.getId(), principal.getRole(), "RECORD_LIST", "/api/medical-records/patient/" + patientId, request);

        List<MedicalRecord> records = recordRepository.findByPatientIdOrderByRecordDateDesc(patientId);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "RECORD_LIST", "MEDICAL_RECORD", patientId, request);

        return records.stream()
                .map(r -> toResponse(r, principal.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse getRecordById(
            String recordId,
            UserPrincipal principal,
            HttpServletRequest request) {

        MedicalRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", recordId));

        Patient patient = patientRepository.findById(record.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", record.getPatientId()));

        verifyReadAccess(patient, principal, request);

        anomalyDetectionService.evaluate(principal.getId(), principal.getRole(), "RECORD_VIEW", "/api/medical-records/" + recordId, request);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "RECORD_VIEW", "MEDICAL_RECORD", recordId, request);

        return toResponse(record, principal.getRole());
    }

    @Transactional
    public MedicalRecordResponse createRecord(
            CreateRecordRequest req,
            UserPrincipal principal,
            HttpServletRequest request) {

        if (principal.getRole() != Role.DOCTOR && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only licensed physicians and administrators can create medical records");
        }

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", req.getPatientId()));

        anomalyDetectionService.evaluate(principal.getId(), principal.getRole(), "RECORD_CREATE", "/api/medical-records", request);

        // AES-256-GCM Encryption of all sensitive medical fields
        String encDiagnosis = encryptionService.encryptField(req.getDiagnosis());
        String encTreatment = encryptionService.encryptField(req.getTreatment());
        String encPrescriptions = encryptionService.encryptField(req.getPrescriptions());
        String encClinicalNotes = encryptionService.encryptField(req.getClinicalNotes());
        String encMedicalHistory = encryptionService.encryptField(req.getMedicalHistory());

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(patient.getId());
        record.setDoctorId(principal.getId());
        record.setRecordType(req.getRecordType());
        record.setRecordDate(LocalDateTime.now());
        record.setEncryptedDiagnosis(encDiagnosis);
        record.setEncryptedTreatment(encTreatment);
        record.setEncryptedPrescriptions(encPrescriptions);
        record.setEncryptedClinicalNotes(encClinicalNotes);
        record.setEncryptedMedicalHistory(encMedicalHistory);
        record.setKeyVersion("v1");
        record.setEncryptionIv("GCM_INLINE");
        record.setEncryptionAuthTag("GCM_INLINE");

        // Compute tamper-evident record hash
        String recordHash = computeHash(patient.getId(), principal.getId(), encDiagnosis);
        record.setRecordHash(recordHash);

        MedicalRecord saved = recordRepository.save(record);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "RECORD_CREATE", "MEDICAL_RECORD", saved.getId(), request);

        return toResponse(saved, principal.getRole());
    }

    @Transactional
    public void deleteRecord(String recordId, UserPrincipal principal, HttpServletRequest request) {
        if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can delete medical records");
        }
        MedicalRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", recordId));

        recordRepository.delete(record);
        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "RECORD_DELETE", "MEDICAL_RECORD", recordId, request);
    }

    private void verifyReadAccess(Patient patient, UserPrincipal principal, HttpServletRequest request) {
        Role role = principal.getRole();

        // ADMIN, DOCTOR, NURSE have role-based access
        if (role == Role.ADMIN || role == Role.DOCTOR || role == Role.NURSE) {
            return;
        }

        // PATIENT can only see their own records
        if (role == Role.PATIENT) {
            if (patient.getUserId() != null && patient.getUserId().equals(principal.getId())) {
                return;
            }
            auditEventService.logFailure(principal.getId(), principal.getEmail(), role.name(),
                    "RECORD_ACCESS_DENIED", "MEDICAL_RECORD", patient.getId(), "Patient unauthorized for record", request);
            throw new ForbiddenException("You can only access your own medical records");
        }

        // RESEARCHER and INSURANCE require explicit active permission
        if (role == Role.RESEARCHER || role == Role.INSURANCE) {
            boolean hasPermission = permissionRepository.findActivePermission(
                    principal.getId(), patient.getId(), "READ").isPresent();
            if (!hasPermission) {
                // Also check if general permission for all actions exists
                hasPermission = !permissionRepository.findActivePermissions(principal.getId(), patient.getId()).isEmpty();
            }
            if (!hasPermission) {
                auditEventService.logFailure(principal.getId(), principal.getEmail(), role.name(),
                        "RECORD_ACCESS_DENIED", "MEDICAL_RECORD", patient.getId(), "No active consent/permission", request);
                throw new ForbiddenException("Access denied: No active consent or permission granted for this patient's records");
            }
        }
    }

    private MedicalRecordResponse toResponse(MedicalRecord record, Role viewerRole) {
        MedicalRecordResponse resp = new MedicalRecordResponse();
        resp.setId(record.getId());
        resp.setPatientId(record.getPatientId());
        resp.setDoctorId(record.getDoctorId());
        resp.setDoctorName(resolveDoctorName(record.getDoctorId()));
        resp.setRecordType(record.getRecordType());
        resp.setRecordDate(record.getRecordDate());
        resp.setKeyVersion(record.getKeyVersion());
        resp.setRecordHash(record.getRecordHash());
        resp.setEncrypted(true);
        resp.setCreatedAt(record.getCreatedAt());

        // Decrypt fields
        String diag = encryptionService.decryptField(record.getEncryptedDiagnosis(), record.getEncryptionIv(), record.getKeyVersion());
        String treat = encryptionService.decryptField(record.getEncryptedTreatment(), record.getEncryptionIv(), record.getKeyVersion());
        String pres = encryptionService.decryptField(record.getEncryptedPrescriptions(), record.getEncryptionIv(), record.getKeyVersion());
        String notes = encryptionService.decryptField(record.getEncryptedClinicalNotes(), record.getEncryptionIv(), record.getKeyVersion());
        String hist = encryptionService.decryptField(record.getEncryptedMedicalHistory(), record.getEncryptionIv(), record.getKeyVersion());

        if (viewerRole == Role.RESEARCHER) {
            // De-identify/redact identifying notes for researchers
            resp.setDiagnosis(diag);
            resp.setTreatment(treat);
            resp.setPrescriptions(pres);
            resp.setClinicalNotes("[ANONYMIZED FOR RESEARCH]");
            resp.setMedicalHistory("[REDACTED FOR RESEARCH]");
        } else {
            resp.setDiagnosis(diag);
            resp.setTreatment(treat);
            resp.setPrescriptions(pres);
            resp.setClinicalNotes(notes);
            resp.setMedicalHistory(hist);
        }

        return resp;
    }

    private String resolveDoctorName(String doctorId) {
        if (doctorId == null) return "Unknown Doctor";
        return doctorNameCache.computeIfAbsent(doctorId, id ->
                userRepository.findById(id)
                        .map(User::getFullName)
                        .orElse("Dr. Staff")
        );
    }

    private String computeHash(String patientId, String doctorId, String encDiagnosis) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String data = patientId + ":" + doctorId + ":" + encDiagnosis;
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
