package com.backend.protection.service;

import com.backend.protection.entity.OtpCode;
import com.backend.protection.entity.User;
import com.backend.protection.repository.OtpRepository;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ─── OtpService ──────────────────────────────────────────────────────────────
 * Generates, validates, and delivers 6-digit One-Time Passwords.
 *
 * Delivery: Real email via EmailService (Gmail SMTP — configured in application-dev.properties).
 * Storage:  Hashed with BCrypt so even the DB admin cannot see the raw OTP.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository   otpRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService    emailService;

    public OtpService(OtpRepository otpRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService) {
        this.otpRepository   = otpRepository;
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService    = emailService;
    }

    /**
     * Generates a new 6-digit OTP for the user, invalidates old ones,
     * stores a BCrypt-hashed version, and delivers it to the user's email.
     *
     * @param userId  The user's UUID string
     * @param purpose LOGIN | REGISTER | FORGOT_PASSWORD
     * @return The plain-text OTP (returned to the caller — remove from API response once email is confirmed working)
     */
    @Transactional
    public String generateOtp(String userId, String purpose) {
        // Invalidate any existing OTPs for this user/purpose
        otpRepository.invalidateAllOtpForUser(userId, purpose);

        // Generate 6-digit OTP
        String otp = String.format("%06d", RANDOM.nextInt(999999));

        // Hash before storage — raw OTP never persisted
        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setOtpHash(passwordEncoder.encode(otp));
        otpCode.setPurpose(purpose);
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(SecurityConstants.OTP_EXPIRY_MINUTES));
        otpRepository.save(otpCode);

        // ── Deliver via Gmail SMTP ─────────────────────────────────────────
        deliverOtpEmail(userId, otp, purpose);
        // ──────────────────────────────────────────────────────────────────

        log.info("OTP generated for user={} purpose={}", userId, purpose);
        return otp; // still returned so it appears in the login API response while you verify delivery is working
    }

    /**
     * Verifies an OTP submitted by the user.
     *
     * @param userId   The user's UUID
     * @param plainOtp The raw OTP entered by the user
     * @param purpose  Must match the purpose used during generation
     * @return true if valid, false otherwise
     */
    @Transactional
    public boolean verifyOtp(String userId, String plainOtp, String purpose) {
        List<OtpCode> otpCodes = otpRepository
                .findByUserIdAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(userId, purpose);

        for (OtpCode code : otpCodes) {
            if (code.isExpired()) continue;

            if (code.getAttempts() >= SecurityConstants.MAX_OTP_ATTEMPTS) {
                code.setUsed(true);
                otpRepository.save(code);
                continue;
            }

            code.setAttempts(code.getAttempts() + 1);

            if (passwordEncoder.matches(plainOtp, code.getOtpHash())) {
                code.setUsed(true);
                otpRepository.save(code);
                log.info("OTP verified successfully for user={}", userId);
                return true;
            } else {
                otpRepository.save(code);
            }
        }

        log.warn("OTP verification failed for user={} purpose={}", userId, purpose);
        return false;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Looks up the user and fires an async OTP email.
     * If the user is not found (edge case), logs a warning and skips delivery.
     */
    private void deliverOtpEmail(String userId, String otp, String purpose) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("OTP email skipped — user not found: {}", userId);
                return;
            }
            String name = (user.getFirstName() != null && !user.getFirstName().isBlank())
                    ? user.getFirstName()
                    : user.getEmail();

            // @Async — returns immediately, sends in background thread
            emailService.sendOtpEmail(user.getEmail(), name, otp, purpose);
            log.info("OTP email queued for delivery → {}", user.getEmail());
        } catch (Exception e) {
            // Email failure must NEVER prevent login from succeeding
            log.error("Failed to queue OTP email for userId={}: {}", userId, e.getMessage());
        }
    }
}
