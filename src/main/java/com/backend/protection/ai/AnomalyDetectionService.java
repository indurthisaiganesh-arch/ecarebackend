package com.backend.protection.ai;

import com.backend.protection.entity.Role;
import com.backend.protection.entity.SecurityEvent;
import com.backend.protection.repository.SecurityEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final AnomalyModel anomalyModel;
    private final SecurityEventRepository securityEventRepository;

    public AnomalyDetectionService(AnomalyModel anomalyModel, SecurityEventRepository securityEventRepository) {
        this.anomalyModel = anomalyModel;
        this.securityEventRepository = securityEventRepository;
    }

    /**
     * Evaluates a request and returns the risk assessment.
     * Core auth/authz continues independently of this service.
     */
    public SecurityRisk evaluate(String userId, Role role, String action,
                                  String resource, HttpServletRequest request) {
        try {
            SecurityRisk risk = anomalyModel.evaluate(userId, role, action, resource, request);
            if (!"ALLOW".equals(risk.getDecision()) || risk.getRiskScore() > 20) {
                logSecurityEvent(userId, action, resource, risk, request);
            }
            return risk;
        } catch (Exception e) {
            log.warn("Anomaly detection failed, allowing request by default: {}", e.getMessage());
            return new SecurityRisk(0.0, "LOW", "ALLOW", java.util.List.of("Anomaly detection service unavailable"));
        }
    }

    @Async
    public void logSecurityEvent(String userId, String eventType, String endpoint,
                                  SecurityRisk risk, HttpServletRequest request) {
        try {
            SecurityEvent event = new SecurityEvent();
            event.setUserId(userId);
            event.setEventType(eventType);
            event.setRiskScore(risk.getRiskScore());
            event.setRiskLevel(risk.getRiskLevel());
            event.setDecision(risk.getDecision());
            event.setReasons(String.join("; ", risk.getReasons()));
            event.setEndpoint(endpoint);
            event.setTimestamp(LocalDateTime.now());
            if (request != null) {
                String xf = request.getHeader("X-Forwarded-For");
                event.setIpAddress(xf != null ? xf.split(",")[0].trim() : request.getRemoteAddr());
                event.setUserAgent(request.getHeader("User-Agent"));
            }
            securityEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to log security event: {}", e.getMessage());
        }
    }
}
