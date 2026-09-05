package com.backend.protection.service;

import com.backend.protection.dto.audit.AuditLogResponse;
import com.backend.protection.entity.AuditLog;
import com.backend.protection.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(log.getId());
        resp.setUserId(log.getUserId());
        resp.setUsername(log.getUsername());
        resp.setRole(log.getRole());
        resp.setAction(log.getAction());
        resp.setResource(log.getResource());
        resp.setResourceId(log.getResourceId());
        resp.setIpAddress(log.getIpAddress());
        resp.setStatus(log.getStatus());
        resp.setReason(log.getReason());
        resp.setTimestamp(log.getTimestamp());
        return resp;
    }
}
