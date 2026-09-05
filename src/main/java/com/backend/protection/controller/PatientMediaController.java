package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.media.PatientMediaResponse;
import com.backend.protection.dto.media.UploadMediaRequest;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.PatientMediaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-media")
public class PatientMediaController {

    private final PatientMediaService mediaService;

    public PatientMediaController(PatientMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PatientMediaResponse>> uploadMedia(
            @Valid @RequestBody UploadMediaRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        PatientMediaResponse response = mediaService.uploadMedia(request, principal, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.created(response, "Diagnostic media and scan uploaded successfully"));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientMediaResponse>>> getMediaByPatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        List<PatientMediaResponse> media = mediaService.getMediaByPatient(patientId, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(media, "Patient medical scans and diagnostics retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientMediaResponse>> getMediaById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        PatientMediaResponse media = mediaService.getMediaById(id, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(media, "Media record retrieved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        mediaService.deleteMedia(id, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Media record deleted"));
    }
}
