package com.backend.protection.dto.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class EmailNotificationRequest {

    private String type;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    private String subject;

    private String body;

    private Map<String, Object> templateData;

    public EmailNotificationRequest() {
    }

    public EmailNotificationRequest(String type, String to, String subject, Map<String, Object> templateData) {
        this.type = type;
        this.to = to;
        this.subject = subject;
        this.templateData = templateData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, Object> getTemplateData() {
        return templateData;
    }

    public void setTemplateData(Map<String, Object> templateData) {
        this.templateData = templateData;
    }
}
