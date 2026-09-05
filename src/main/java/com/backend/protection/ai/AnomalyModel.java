package com.backend.protection.ai;

import com.backend.protection.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Heuristic anomaly detection model.
 * Evaluates multiple security risk signals to produce a composite risk score.
 * This is an additive/rule-based ML-inspired model - independent of core auth/authz.
 */
@Component
public class AnomalyModel {

    public SecurityRisk evaluate(String userId, Role role, String action,
                                  String resource, HttpServletRequest request) {
        List<String> reasons = new ArrayList<>();
        double score = 0.0;

        // 1. Time-of-day risk (unusual hours: 11PM - 5AM)
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(23, 0)) || now.isBefore(LocalTime.of(5, 0))) {
            score += 15.0;
            reasons.add("Access outside normal business hours");
        }

        // 2. Role risk scoring
        if (role != null) {
            score += switch (role) {
                case RESEARCHER, INSURANCE -> 10.0;
                case PATIENT -> 5.0;
                default -> 0.0;
            };
        }

        // 3. Resource sensitivity
        if ("MEDICAL_RECORD".equals(resource)) {
            score += 10.0;
        }

        // 4. Operation risk
        if ("DELETE".equals(action)) {
            score += 20.0;
            reasons.add("High-risk DELETE operation");
        } else if ("EXPORT".equals(action)) {
            score += 15.0;
            reasons.add("Data export operation");
        }

        // 5. IP risk (simplified - in production connect to IP reputation service)
        if (request != null) {
            String ip = getClientIp(request);
            if (ip != null && ip.startsWith("10.") || (ip != null && ip.startsWith("192.168."))) {
                // Internal IP - lower risk
                score -= 5.0;
            }
        }

        score = Math.max(0.0, Math.min(100.0, score));

        String level;
        String decision;
        if (score >= 75.0) {
            level = "CRITICAL";
            decision = "BLOCK";
            reasons.add("Risk score exceeds blocking threshold");
        } else if (score >= 50.0) {
            level = "HIGH";
            decision = "ALERT";
        } else if (score >= 25.0) {
            level = "MEDIUM";
            decision = "ALLOW";
        } else {
            level = "LOW";
            decision = "ALLOW";
        }

        return new SecurityRisk(score, level, decision, reasons);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
