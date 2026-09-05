package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.user.ChangePasswordRequest;
import com.backend.protection.dto.user.UpdateUserRequest;
import com.backend.protection.dto.user.UserResponse;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.ForbiddenException;
import com.backend.protection.exception.ResourceNotFoundException;
import com.backend.protection.exception.UnauthorizedException;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UserPrincipal principal, UpdateUserRequest req, HttpServletRequest httpRequest) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());

        User saved = userRepository.save(user);
        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PROFILE_UPDATE", "USER", saved.getId(), httpRequest);

        return toResponse(saved);
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest req, HttpServletRequest httpRequest) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            auditEventService.logFailure(principal.getId(), principal.getEmail(), principal.getRole().name(),
                    "PASSWORD_CHANGE", "USER", principal.getId(), "Current password incorrect", httpRequest);
            throw new UnauthorizedException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                "PASSWORD_CHANGE", "USER", principal.getId(), httpRequest);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(UserPrincipal principal) {
        if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can view the user list");
        }
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse toggleUserActive(String userId, boolean active, UserPrincipal principal, HttpServletRequest httpRequest) {
        if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can activate/deactivate users");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setActive(active);
        User saved = userRepository.save(user);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                active ? "USER_ACTIVATE" : "USER_DEACTIVATE", "USER", userId, httpRequest);

        return toResponse(saved);
    }

    @Transactional
    public UserResponse toggleUserMfa(String userId, boolean enabled, UserPrincipal principal, HttpServletRequest httpRequest) {
        if (principal.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can change 2FA settings");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setMfaEnabled(enabled);
        User saved = userRepository.save(user);

        auditEventService.logSuccess(principal.getId(), principal.getEmail(), principal.getRole().name(),
                enabled ? "2FA_ENABLED" : "2FA_DISABLED", "USER", userId, httpRequest);

        return toResponse(saved);
    }

    public UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setFirstName(u.getFirstName());
        r.setLastName(u.getLastName());
        r.setFullName(u.getFullName());
        r.setRole(u.getRole());
        r.setPhoneNumber(u.getPhoneNumber());
        r.setActive(u.isActive());
        r.setLocked(u.getLockoutUntil() != null && java.time.LocalDateTime.now().isBefore(u.getLockoutUntil()));
        r.setMfaEnabled(u.isMfaEnabled());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}
