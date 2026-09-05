package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.patient.*;
import com.backend.protection.entity.Patient;
import com.backend.protection.entity.Role;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.exception.ResourceNotFoundException;
import com.backend.protection.repository.PatientRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditEventService auditEventService;

    public PatientService(PatientRepository patientRepository, AuditEventService auditEventService) {
        this.patientRepository = patientRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(UserPrincipal principal, String query, Pageable pageable, HttpServletRequest request) {
        Page<Patient> patients;
        if (query != null && !query.isBlank()) {
            patients = patientRepository.searchPatients(query, pageable);
        } else if (principal.getRole() == Role.PATIENT) {
            // Patients can only see themselves
            var p = patientRepository.findByUserId(principal.getId()).orElse(null);
            patients = p == null ? Page.empty(pageable) : new org.springframework.data.domain.PageImpl<>(java.util.List.of(p), pageable, 1);
        } else {
            patients = patientRepository.findAll(pageable);
        }
        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PATIENT_LIST", "PATIENT", null, request);
        return patients.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(String id, UserPrincipal principal, HttpServletRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        // PATIENT role can only see their own profile
        if (principal.getRole() == Role.PATIENT && !principal.getId().equals(patient.getUserId())) {
            auditEventService.logFailure(principal.getId(), principal.getEmail(), principal.getRole().name(),
                    "PATIENT_VIEW", "PATIENT", id, "Access denied - patient viewing other patient", request);
            throw new ForbiddenException("You can only view your own profile");
        }

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PATIENT_VIEW", "PATIENT", id, request);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, UserPrincipal principal, HttpServletRequest httpRequest) {
        Patient patient = new Patient();
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setContactPhone(request.getContactPhone());
        patient.setContactEmail(request.getContactEmail());
        patient.setAddress(request.getAddress());
        patient.setEmergencyContactName(request.getEmergencyContactName());
        patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        patient.setCreatedBy(principal.getId());
        patient.setUserId(request.getUserId());

        Patient saved = patientRepository.save(patient);
        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PATIENT_CREATE", "PATIENT", saved.getId(), httpRequest);
        return toResponse(saved);
    }

    @Transactional
    public PatientResponse updatePatient(String id, UpdatePatientRequest request, UserPrincipal principal, HttpServletRequest httpRequest) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        if (principal.getRole() == Role.PATIENT && !principal.getId().equals(patient.getUserId())) {
            throw new ForbiddenException("You can only update your own profile");
        }

        if (request.getFirstName() != null) patient.setFirstName(request.getFirstName());
        if (request.getLastName() != null) patient.setLastName(request.getLastName());
        if (request.getContactPhone() != null) patient.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) patient.setContactEmail(request.getContactEmail());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        if (request.getEmergencyContactName() != null) patient.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getBloodGroup() != null) patient.setBloodGroup(request.getBloodGroup());

        Patient updated = patientRepository.save(patient);
        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PATIENT_UPDATE", "PATIENT", id, httpRequest);
        return toResponse(updated);
    }

    @Transactional
    public void deletePatient(String id, UserPrincipal principal, HttpServletRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
        patientRepository.delete(patient);
        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PATIENT_DELETE", "PATIENT", id, request);
    }

    public PatientResponse toResponse(Patient p) {
        PatientResponse r = new PatientResponse();
        r.setId(p.getId());
        r.setPatientIdentifier(p.getPatientIdentifier());
        r.setFirstName(p.getFirstName());
        r.setLastName(p.getLastName());
        r.setDateOfBirth(p.getDateOfBirth());
        r.setGender(p.getGender());
        r.setBloodGroup(p.getBloodGroup());
        r.setContactPhone(p.getContactPhone());
        r.setContactEmail(p.getContactEmail());
        r.setAddress(p.getAddress());
        r.setEmergencyContactName(p.getEmergencyContactName());
        r.setEmergencyContactPhone(p.getEmergencyContactPhone());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
