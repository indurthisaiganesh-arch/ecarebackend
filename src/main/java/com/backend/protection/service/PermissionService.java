package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.permission.GrantPermissionRequest;
import com.backend.protection.dto.permission.PermissionResponse;
import com.backend.protection.entity.Patient;
import com.backend.protection.entity.Permission;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.exception.ResourceNotFoundException;
import com.backend.protection.repository.PatientRepository;
import com.backend.protection.repository.PermissionRepository;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AuditEventService auditEventService;

    public PermissionService(
            PermissionRepository permissionRepository,
            UserRepository userRepository,
            PatientRepository patientRepository,
            AuditEventService auditEventService) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public PermissionResponse grantPermission(
            GrantPermissionRequest req,
            UserPrincipal principal,
            HttpServletRequest request) {

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", req.getPatientId()));

        User targetUser = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));

        // Patients can only grant permissions on their own data
        if (principal.getRole() == Role.PATIENT) {
            if (patient.getUserId() == null || !patient.getUserId().equals(principal.getId())) {
                throw new ForbiddenException("You can only grant access permissions for your own patient profile");
            }
        } else if (principal.getRole() != Role.ADMIN && principal.getRole() != Role.DOCTOR) {
            throw new ForbiddenException("Unauthorized to grant permissions");
        }

        Permission perm = new Permission();
        perm.setUserId(targetUser.getId());
        perm.setPatientId(patient.getId());
        perm.setResourceType(req.getResourceType() != null ? req.getResourceType() : "MEDICAL_RECORD");
        perm.setAction(req.getAction() != null ? req.getAction() : "READ");
        perm.setGrantedBy(principal.getId());
        perm.setStatus("ACTIVE");
        perm.setExpiresAt(LocalDateTime.now().plusHours(req.getExpiresInHours() != null ? req.getExpiresInHours() : 24));
        perm.setReason(req.getReason());

        Permission saved = permissionRepository.save(perm);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PERMISSION_GRANT", "PERMISSION", saved.getId(), request);

        return toResponse(saved, targetUser, patient);
    }

    @Transactional
    public void revokePermission(String permissionId, UserPrincipal principal, HttpServletRequest request) {
        Permission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));

        if (principal.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findById(perm.getPatientId()).orElse(null);
            if (patient == null || !principal.getId().equals(patient.getUserId())) {
                throw new ForbiddenException("You can only revoke permissions for your own records");
            }
        } else if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only patients or administrators can revoke permissions");
        }

        perm.setStatus("REVOKED");
        permissionRepository.save(perm);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PERMISSION_REVOKE", "PERMISSION", permissionId, request);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsForPatient(String patientId, UserPrincipal principal) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        if (principal.getRole() == Role.PATIENT && (patient.getUserId() == null || !patient.getUserId().equals(principal.getId()))) {
            throw new ForbiddenException("Access denied");
        }

        return permissionRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions(UserPrincipal principal) {
        if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can view all system permissions");
        }
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PermissionResponse toResponse(Permission perm) {
        User user = userRepository.findById(perm.getUserId()).orElse(null);
        Patient patient = patientRepository.findById(perm.getPatientId()).orElse(null);
        return toResponse(perm, user, patient);
    }

    private PermissionResponse toResponse(Permission perm, User user, Patient patient) {
        PermissionResponse resp = new PermissionResponse();
        resp.setId(perm.getId());
        resp.setUserId(perm.getUserId());
        resp.setUserName(user != null ? user.getFullName() : "Unknown");
        resp.setUserEmail(user != null ? user.getEmail() : "Unknown");
        resp.setUserRole(user != null && user.getRole() != null ? user.getRole().name() : "");
        resp.setPatientId(perm.getPatientId());
        resp.setPatientName(patient != null ? patient.getFirstName() + " " + patient.getLastName() : "Unknown");
        resp.setResourceType(perm.getResourceType());
        resp.setAction(perm.getAction());
        resp.setGrantedBy(perm.getGrantedBy());
        resp.setStatus(perm.isExpired() ? "EXPIRED" : perm.getStatus());
        resp.setExpiresAt(perm.getExpiresAt());
        resp.setReason(perm.getReason());
        resp.setCreatedAt(perm.getCreatedAt());
        return resp;
    }
}
