package com.backend.protection.service;

import com.backend.protection.audit.AuditEventService;
import com.backend.protection.dto.auth.*;
import com.backend.protection.entity.RefreshToken;
import com.backend.protection.entity.Role;
import com.backend.protection.entity.User;
import com.backend.protection.exception.UnauthorizedException;
import com.backend.protection.repository.RefreshTokenRepository;
import com.backend.protection.repository.UserRepository;
import com.backend.protection.security.JwtService;
import com.backend.protection.security.SecurityConstants;
import com.backend.protection.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuditEventService auditEventService;

    @Value("${app.security.jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    @Value("${app.security.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    // Temporary OTP storage for display on screen (email will replace this)
    private final Map<String, String> pendingOtpMap = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       OtpService otpService, AuditEventService auditEventService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());

        // Default role is PATIENT; ADMIN can set a different role via admin API
        Role role = Role.PATIENT;
        if (request.getRole() != null) {
            try { role = Role.valueOf(request.getRole().toUpperCase()); } catch (Exception ignored) {}
        }
        user.setRole(role);
        User saved = userRepository.save(user);

        auditEventService.logSuccess(saved.getId(), saved.getEmail(), role.name(),
                "USER_REGISTER", "USER", saved.getId(), httpRequest);

        return Map.of("userId", saved.getId(), "email", saved.getEmail(), "role", saved.getRole().name(),
                      "message", "Registration successful. Please verify your email with OTP.");
    }

    @Transactional
    public Map<String, Object> initiateLogin(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) throw new UnauthorizedException("Account is disabled");

        if (user.getLockoutUntil() != null && LocalDateTime.now().isBefore(user.getLockoutUntil())) {
            throw new UnauthorizedException("Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= SecurityConstants.MAX_FAILED_ATTEMPTS) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(SecurityConstants.LOCKOUT_DURATION_MINUTES));
            }
            userRepository.save(user);
            auditEventService.logFailure(user.getId(), user.getEmail(), user.getRole().name(),
                    "LOGIN_FAILURE", "AUTH", null, "Invalid password", httpRequest);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Reset failed attempts on successful password
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        // If 2FA is disabled for this user, skip OTP and issue tokens immediately
        if (!user.isMfaEnabled()) {
            auditEventService.logSuccess(user.getId(), user.getEmail(), user.getRole().name(),
                    "LOGIN_SUCCESS_NO_2FA", "AUTH", user.getId(), httpRequest);
            AuthResponse authResponse = buildAuthResponse(user, httpRequest);
            return Map.of(
                "requiresOtp", false,
                "email", user.getEmail(),
                "accessToken", authResponse.getAccessToken(),
                "refreshToken", authResponse.getRefreshToken(),
                "expiresIn", authResponse.getExpiresIn(),
                "user", authResponse.getUser(),
                "message", "Login successful (2FA bypassed by administrator)"
            );
        }

        // Generate OTP (2FA enabled path)
        String otp = otpService.generateOtp(user.getId(), SecurityConstants.OTP_PURPOSE_LOGIN);
        pendingOtpMap.put(user.getEmail(), otp); // store for display

        log.info("OTP generated for login: user={}", user.getEmail());

        return Map.of("requiresOtp", true,
                      "email", user.getEmail(), "otpSent", true,
                      "tempOtp", otp, // shown on screen; remove when email configured
                      "message", "OTP sent. Please verify to complete login.",
                      "expiresInMinutes", SecurityConstants.OTP_EXPIRY_MINUTES);
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(OtpRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email"));

        String purpose = request.getPurpose() != null ? request.getPurpose() : SecurityConstants.OTP_PURPOSE_LOGIN;

        if (!otpService.verifyOtp(user.getId(), request.getOtp(), purpose)) {
            auditEventService.logFailure(user.getId(), user.getEmail(), user.getRole().name(),
                    "OTP_FAILURE", "AUTH", null, "Invalid or expired OTP", httpRequest);
            throw new UnauthorizedException("Invalid or expired OTP");
        }

        pendingOtpMap.remove(user.getEmail());
        auditEventService.logSuccess(user.getId(), user.getEmail(), user.getRole().name(),
                "LOGIN_SUCCESS", "AUTH", user.getId(), httpRequest);

        return buildAuthResponse(user, httpRequest);
    }

    @Transactional
    public Map<String, Object> sendOtp(String email, String purpose, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Email not found"));

        String otp = otpService.generateOtp(user.getId(), purpose);
        pendingOtpMap.put(email, otp);

        return Map.of("email", email, "otpSent", true,
                      "tempOtp", otp,
                      "message", "OTP sent",
                      "expiresInMinutes", SecurityConstants.OTP_EXPIRY_MINUTES);
    }

    @Transactional
    public AuthResponse refreshTokens(String rawRefreshToken, HttpServletRequest httpRequest) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (!token.isValid()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildAuthResponse(user, httpRequest);
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.revokeAllTokensForUser(userId);
        log.info("User {} logged out - all refresh tokens revoked", userId);
    }

    private AuthResponse buildAuthResponse(User user, HttpServletRequest httpRequest) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateToken(principal);

        // Create refresh token
        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        if (httpRequest != null) {
            refreshToken.setIpAddress(httpRequest.getRemoteAddr());
            refreshToken.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        }
        refreshTokenRepository.save(refreshToken);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setUsername(user.getUsername());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setRole(user.getRole().name());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(rawRefreshToken);
        response.setExpiresIn(jwtExpirationMs / 1000);
        response.setUser(userInfo);

        return response;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Token hashing failed", e);
        }
    }
}
