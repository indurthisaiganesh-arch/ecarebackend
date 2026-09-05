package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.media.PatientMediaResponse;
import com.backend.protection.dto.media.UploadMediaRequest;
import com.backend.protection.entity.Patient;
import com.backend.protection.entity.PatientMedia;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.exception.ResourceNotFoundException;
import com.backend.protection.repository.PatientMediaRepository;
import com.backend.protection.repository.PatientRepository;
import com.backend.protection.repository.PermissionRepository;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientMediaService {

    private final PatientMediaRepository mediaRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final AuditEventService auditEventService;

    public PatientMediaService(
            PatientMediaRepository mediaRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            PermissionRepository permissionRepository,
            AuditEventService auditEventService) {
        this.mediaRepository = mediaRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public PatientMediaResponse uploadMedia(UploadMediaRequest req, UserPrincipal principal, HttpServletRequest request) {
        Role role = principal.getRole();
        if (role != Role.LAB_TECHNICIAN && role != Role.RADIOLOGIST && role != Role.DOCTOR && role != Role.ADMIN) {
            throw new ForbiddenException("Only lab technicians, radiologists, doctors, and administrators can upload diagnostic media");
        }

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", req.getPatientId()));

        PatientMedia media = new PatientMedia();
        media.setPatientId(patient.getId());
        media.setUploaderId(principal.getId());
        media.setRecordId(req.getRecordId());
        media.setMediaType(req.getMediaType());
        media.setTitle(req.getTitle());
        media.setDescription(req.getDescription());
        media.setFileName(req.getFileName());
        media.setFileType(req.getFileType());
        media.setFileSize(req.getFileSize());
        media.setFileData(req.getFileData());

        // SHA-256 Integrity Digest
        String mediaHash = computeHash(patient.getId(), req.getTitle(), req.getFileName(), req.getFileSize());
        media.setMediaHash(mediaHash);

        PatientMedia saved = mediaRepository.save(media);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), role.name(),
                "MEDIA_UPLOAD", "PATIENT_MEDIA", saved.getId(), request);

        return toResponse(saved, patient, principal.getFullName(), role.name());
    }

    @Transactional(readOnly = true)
    public List<PatientMediaResponse> getMediaByPatient(String patientId, UserPrincipal principal, HttpServletRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        verifyMediaAccess(patient, principal, request);

        List<PatientMedia> mediaList = mediaRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "MEDIA_LIST", "PATIENT_MEDIA", patientId, request);

        return mediaList.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientMediaResponse getMediaById(String mediaId, UserPrincipal principal, HttpServletRequest request) {
        PatientMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientMedia", mediaId));

        Patient patient = patientRepository.findById(media.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", media.getPatientId()));

        verifyMediaAccess(patient, principal, request);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "MEDIA_VIEW", "PATIENT_MEDIA", mediaId, request);

        return toResponse(media);
    }

    @Transactional
    public void deleteMedia(String mediaId, UserPrincipal principal, HttpServletRequest request) {
        if (principal.getRole() != Role.ADMIN && principal.getRole() != Role.DOCTOR) {
            throw new ForbiddenException("Only physicians and administrators can delete patient media");
        }

        PatientMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientMedia", mediaId));

        mediaRepository.delete(media);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "MEDIA_DELETE", "PATIENT_MEDIA", mediaId, request);
    }

    private void verifyMediaAccess(Patient patient, UserPrincipal principal, HttpServletRequest request) {
        Role role = principal.getRole();

        // Clinical / Hospital staff have access
        if (role == Role.ADMIN || role == Role.DOCTOR || role == Role.NURSE || 
            role == Role.LAB_TECHNICIAN || role == Role.RADIOLOGIST || role == Role.RECEPTIONIST) {
            return;
        }

        // Patient can view own media
        if (role == Role.PATIENT) {
            if (patient.getUserId() != null && patient.getUserId().equals(principal.getId())) {
                return;
            }
            throw new ForbiddenException("You can only access your own diagnostic scans and lab reports");
        }

        // Researcher & Insurance require active consent
        if (role == Role.RESEARCHER || role == Role.INSURANCE) {
            boolean hasPermission = !permissionRepository.findActivePermissions(principal.getId(), patient.getId()).isEmpty();
            if (!hasPermission) {
                throw new ForbiddenException("Access denied: No active consent granted to view this patient's medical scans");
            }
        }
    }

    private PatientMediaResponse toResponse(PatientMedia m) {
        Patient patient = patientRepository.findById(m.getPatientId()).orElse(null);
        User uploader = userRepository.findById(m.getUploaderId()).orElse(null);
        String uploaderName = uploader != null ? uploader.getFullName() : "Clinical Staff";
        String uploaderRole = uploader != null && uploader.getRole() != null ? uploader.getRole().name() : "";
        return toResponse(m, patient, uploaderName, uploaderRole);
    }

    private PatientMediaResponse toResponse(PatientMedia m, Patient patient, String uploaderName, String uploaderRole) {
        PatientMediaResponse resp = new PatientMediaResponse();
        resp.setId(m.getId());
        resp.setPatientId(m.getPatientId());
        resp.setPatientName(patient != null ? patient.getFirstName() + " " + patient.getLastName() : "Unknown");
        resp.setUploaderId(m.getUploaderId());
        resp.setUploaderName(uploaderName);
        resp.setUploaderRole(uploaderRole);
        resp.setRecordId(m.getRecordId());
        resp.setMediaType(m.getMediaType());
        resp.setTitle(m.getTitle());
        resp.setDescription(m.getDescription());
        resp.setFileName(m.getFileName());
        resp.setFileType(m.getFileType());
        resp.setFileSize(m.getFileSize());
        resp.setFileData(m.getFileData());
        resp.setMediaHash(m.getMediaHash());
        resp.setCreatedAt(m.getCreatedAt());
        return resp;
    }

    private String computeHash(String patientId, String title, String fileName, Long fileSize) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String data = patientId + ":" + title + ":" + fileName + ":" + fileSize;
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "MEDIA_HASH_ERROR";
        }
    }
}
