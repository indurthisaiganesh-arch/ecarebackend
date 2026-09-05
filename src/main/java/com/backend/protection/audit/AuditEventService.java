package com.backend.protection.audit;

import com.backend.protection.entity.AuditLog;
import com.backend.protection.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditEventService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void logEvent(String userId, String username, String role, String action,
                         String resource, String resourceId, String ipAddress,
                         String userAgent, String status, String reason) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setRole(role);
            log.setAction(action);
            log.setResource(resource);
            log.setResourceId(resourceId);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setStatus(status);
            log.setReason(reason);
            log.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            AuditEventService.log.error("Failed to log audit event: {}", e.getMessage());
        }
    }

    public void logSuccess(String userId, String username, String role, String action,
                           String resource, String resourceId, HttpServletRequest request) {
        String ip = getClientIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : "SYSTEM";
        logEvent(userId, username, role, action, resource, resourceId, ip, ua, "SUCCESS", null);
    }

    public void logFailure(String userId, String username, String role, String action,
                           String resource, String resourceId, String reason, HttpServletRequest request) {
        String ip = getClientIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : "SYSTEM";
        logEvent(userId, username, role, action, resource, resourceId, ip, ua, "FAILURE", reason);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "INTERNAL";
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader != null) ? xfHeader.split(",")[0].trim() : request.getRemoteAddr();
    }
}
