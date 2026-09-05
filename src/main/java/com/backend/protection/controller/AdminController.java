package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.entity.SecurityEvent;
import com.backend.protection.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository recordRepository;
    private final PermissionRepository permissionRepository;
    private final SecurityEventRepository securityEventRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(
            UserRepository userRepository,
            PatientRepository patientRepository,
            MedicalRecordRepository recordRepository,
            PermissionRepository permissionRepository,
            SecurityEventRepository securityEventRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.recordRepository = recordRepository;
        this.permissionRepository = permissionRepository;
        this.securityEventRepository = securityEventRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalMedicalRecords", recordRepository.count());
        stats.put("totalPermissions", permissionRepository.count());
        stats.put("totalAuditLogs", auditLogRepository.count());
        stats.put("totalSecurityEvents", securityEventRepository.count());

        Map<String, Object> securityStatus = new HashMap<>();
        securityStatus.put("encryptionAlgorithm", "AES-256-GCM (Authenticated)");
        securityStatus.put("keyAgreement", "ECC (secp256r1 / NIST P-256 via Bouncy Castle)");
        securityStatus.put("hashingAlgorithm", "BCrypt (cost factor 12) & SHA-256");
        securityStatus.put("tokenType", "Stateless HMAC-SHA256 JWT");
        securityStatus.put("mfaMethod", "HMAC-SHA256 Cryptographic OTP");
        securityStatus.put("anomalyDetection", "Active (Zero-Trust Heuristic Engine)");
        stats.put("securityEngine", securityStatus);

        return ResponseEntity.ok(ApiResponse.success(stats, "Admin dashboard statistics retrieved"));
    }

    @GetMapping("/security-events")
    public ResponseEntity<ApiResponse<Page<SecurityEvent>>> getSecurityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<SecurityEvent> events = securityEventRepository.findByOrderByTimestampDesc(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(events, "Security anomaly events retrieved"));
    }
}
