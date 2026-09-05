package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.staff.AppointStaffRequest;
import com.backend.protection.dto.user.UserResponse;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffRecruitmentService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuditEventService auditEventService;

    public StaffRecruitmentService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserService userService,
            AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public UserResponse appointStaff(AppointStaffRequest req, UserPrincipal principal, HttpServletRequest request) {
        if (principal.getRole() != Role.HEAD_RECRUITER && principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the hospital Head Recruiter and Administrators can appoint new clinical staff");
        }

        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A staff member with this email already exists: " + req.getEmail());
        }

        String firstName = req.getFirstName();
        String lastName = req.getLastName();
        if ((firstName == null || firstName.isBlank()) && req.getFullName() != null && !req.getFullName().isBlank()) {
            String[] parts = req.getFullName().trim().split("\\s+", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : parts[0];
        }
        if (firstName == null || firstName.isBlank()) {
            firstName = "Staff";
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = "Member";
        }

        String username = req.getUsername();
        if (username == null || username.isBlank()) {
            username = req.getEmail().split("@")[0].toLowerCase().replaceAll("[^a-z0-9_.]", "");
            if (userRepository.findByUsername(username).isPresent()) {
                username = username + "_" + (int)(Math.random() * 9000 + 1000);
            }
        } else if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("A staff member with this username already exists: " + username);
        }

        User staff = new User();
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setEmail(req.getEmail());
        staff.setUsername(username);
        staff.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        staff.setRole(req.getRole());
        staff.setPhoneNumber(req.getPhoneNumber());
        staff.setActive(true);
        staff.setMfaEnabled(false);

        User saved = userRepository.save(staff);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "STAFF_APPOINTED", "USER", saved.getId(), request);

        return userService.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getHospitalStaff(UserPrincipal principal) {
        // Returns all staff (excluding generic patients)
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.PATIENT)
                .map(userService::toResponse)
                .collect(Collectors.toList());
    }
}
