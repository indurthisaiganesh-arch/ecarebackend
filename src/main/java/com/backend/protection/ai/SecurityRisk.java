package com.backend.protection.ai;

import java.util.List;

public class SecurityRisk {
    private final double riskScore;
    private final String riskLevel;
    private final String decision;
    private final List<String> reasons;

    public SecurityRisk(double riskScore, String riskLevel, String decision, List<String> reasons) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.reasons = reasons;
    }

    public double getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getDecision() { return decision; }
    public List<String> getReasons() { return reasons; }
    public boolean isAllowed() { return "ALLOW".equals(decision) || "ALERT".equals(decision); }
    public boolean isBlocked() { return "BLOCK".equals(decision); }
}
