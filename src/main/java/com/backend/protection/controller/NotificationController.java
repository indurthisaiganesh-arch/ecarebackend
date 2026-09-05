package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.notification.EmailNotificationRequest;
import com.backend.protection.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final EmailService emailService;

    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Void>> sendEmailNotification(@Valid @RequestBody EmailNotificationRequest request) {
        String type = request.getType() != null ? request.getType().toUpperCase() : "CUSTOM";
        String to = request.getTo();
        Map<String, Object> data = request.getTemplateData() != null ? request.getTemplateData() : Map.of();

        log.info("Received email dispatch request: type={}, to={}", type, to);

        try {
            switch (type) {
                case "WELCOME" -> {
                    String name = getString(data, "name", "User");
                    String role = getString(data, "role", "Staff");
                    emailService.sendWelcomeEmail(to, name, role);
                }
                case "ACCOUNT_APPROVED" -> {
                    String name = getString(data, "name", "User");
                    emailService.sendApprovalEmail(to, name);
                }
                case "ACCOUNT_REJECTED" -> {
                    String name = getString(data, "name", "User");
                    String reason = getString(data, "reason", "Your registration did not meet access requirements.");
                    emailService.sendRejectionEmail(to, name, reason);
                }
                case "APPOINTMENT_CONFIRMED" -> {
                    String patientName = getString(data, "patientName", "Patient");
                    String doctorName = getString(data, "doctorName", "Doctor");
                    String date = getString(data, "appointmentDate", "") + " " + getString(data, "appointmentTime", "");
                    String department = getString(data, "department", "General");
                    String notes = getString(data, "notes", "");
                    emailService.sendAppointmentConfirmationEmail(to, patientName, doctorName, date.trim(), department, notes);
                }
                case "APPOINTMENT_CANCELLED" -> {
                    String patientName = getString(data, "patientName", "Patient");
                    String doctorName = getString(data, "doctorName", "Doctor");
                    String date = getString(data, "appointmentDate", "Scheduled date");
                    emailService.sendAppointmentCancellationEmail(to, patientName, doctorName, date);
                }
                case "OTP_CODE" -> {
                    String name = getString(data, "name", to);
                    String otp = getString(data, "otpCode", "");
                    String purpose = getString(data, "purpose", "LOGIN");
                    emailService.sendOtpEmail(to, name, otp, purpose);
                }
                case "PASSWORD_RESET" -> {
                    String name = getString(data, "name", "User");
                    String resetToken = getString(data, "resetToken", "");
                    emailService.sendPasswordResetEmail(to, name, resetToken);
                }
                default -> {
                    String subject = request.getSubject() != null ? request.getSubject() : "E-Care Digital Notification";
                    String body = request.getBody() != null ? request.getBody() : "<p>Notification from E-Care Digital</p>";
                    emailService.sendHtml(to, subject, body);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process email notification: {}", e.getMessage(), e);
            // Non-blocking response
        }

        return ResponseEntity.ok(ApiResponse.success(null, "Email notification queued for delivery"));
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }
}
