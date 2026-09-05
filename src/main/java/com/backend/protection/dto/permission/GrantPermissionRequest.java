package com.backend.protection.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GrantPermissionRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    private String resourceType = "MEDICAL_RECORD";

    @NotBlank(message = "Action is required")
    private String action = "READ";

    @NotNull(message = "Expiration in hours is required")
    private Integer expiresInHours = 24;

    private String reason;

    public GrantPermissionRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Integer getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(Integer expiresInHours) { this.expiresInHours = expiresInHours; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
