package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.appointment.AppointmentResponse;
import com.backend.protection.dto.appointment.CreateAppointmentRequest;
import com.backend.protection.entity.Appointment;
import com.backend.protection.entity.Patient;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.exception.ResourceNotFoundException;
import com.backend.protection.repository.AppointmentRepository;
import com.backend.protection.repository.PatientRepository;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditEventService auditEventService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            AuditEventService auditEventService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public AppointmentResponse createAppointment(
            CreateAppointmentRequest req,
            UserPrincipal principal,
            HttpServletRequest request) {

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", req.getPatientId()));

        User doctor = userRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", req.getDoctorId()));

        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Target user is not a licensed physician");
        }

        Appointment appt = new Appointment();
        appt.setPatientId(patient.getId());
        appt.setDoctorId(doctor.getId());
        appt.setAppointedBy(principal.getId());
        appt.setAppointmentDate(req.getAppointmentDate());
        appt.setDepartment(req.getDepartment());
        appt.setReason(req.getReason());
        appt.setNotes(req.getNotes());
        appt.setStatus("SCHEDULED");

        Appointment saved = appointmentRepository.save(appt);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "APPOINTMENT_CREATE", "APPOINTMENT", saved.getId(), request);

        return toResponse(saved, patient, doctor, principal.getFullName());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(UserPrincipal principal) {
        Role role = principal.getRole();

        List<Appointment> list;
        if (role == Role.DOCTOR) {
            list = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(principal.getId());
        } else if (role == Role.PATIENT) {
            Patient p = patientRepository.findByUserId(principal.getId()).orElse(null);
            if (p != null) {
                list = appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(p.getId());
            } else {
                list = List.of();
            }
        } else {
            // Receptionist, Admin, etc. can view all
            list = appointmentRepository.findAll();
        }

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse updateStatus(
            String id,
            String newStatus,
            UserPrincipal principal,
            HttpServletRequest request) {

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        appt.setStatus(newStatus.toUpperCase());
        Appointment saved = appointmentRepository.save(appt);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "APPOINTMENT_STATUS_UPDATE", "APPOINTMENT", saved.getId(), request);

        return toResponse(saved);
    }

    private AppointmentResponse toResponse(Appointment a) {
        Patient patient = patientRepository.findById(a.getPatientId()).orElse(null);
        User doctor = userRepository.findById(a.getDoctorId()).orElse(null);
        User appointer = userRepository.findById(a.getAppointedBy()).orElse(null);
        String appointerName = appointer != null ? appointer.getFullName() : "Reception Staff";
        return toResponse(a, patient, doctor, appointerName);
    }

    private AppointmentResponse toResponse(Appointment a, Patient patient, User doctor, String appointerName) {
        AppointmentResponse resp = new AppointmentResponse();
        resp.setId(a.getId());
        resp.setPatientId(a.getPatientId());
        resp.setPatientName(patient != null ? patient.getFirstName() + " " + patient.getLastName() : "Unknown");
        resp.setPatientIdentifier(patient != null ? patient.getPatientIdentifier() : "");
        resp.setDoctorId(a.getDoctorId());
        resp.setDoctorName(doctor != null ? doctor.getFullName() : "Dr. Staff");
        resp.setAppointedBy(a.getAppointedBy());
        resp.setAppointedByName(appointerName);
        resp.setAppointmentDate(a.getAppointmentDate());
        resp.setStatus(a.getStatus());
        resp.setDepartment(a.getDepartment());
        resp.setReason(a.getReason());
        resp.setNotes(a.getNotes());
        resp.setCreatedAt(a.getCreatedAt());
        return resp;
    }
}
