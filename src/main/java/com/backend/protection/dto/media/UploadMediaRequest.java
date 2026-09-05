package com.backend.protection.dto.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UploadMediaRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    private String recordId;

    @NotBlank(message = "Media type is required")
    private String mediaType; // SCAN_XRAY, SCAN_MRI, SCAN_CT, SCAN_ULTRASOUND, LAB_REPORT

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File type is required")
    private String fileType; // image/png, image/jpeg, application/pdf

    @NotNull(message = "File size is required")
    private Long fileSize;

    @NotBlank(message = "File data is required")
    private String fileData; // Base64 data URL

    public UploadMediaRequest() {}

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
}
