package com.backend.protection.config;

import com.backend.protection.encryption.AesEncryptionService;
import com.backend.protection.entity.*;
import com.backend.protection.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository recordRepository;
    private final PermissionRepository permissionRepository;
    private final PatientMediaRepository mediaRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionService encryptionService;

    public DatabaseSeeder(
            UserRepository userRepository,
            PatientRepository patientRepository,
            MedicalRecordRepository recordRepository,
            PermissionRepository permissionRepository,
            PatientMediaRepository mediaRepository,
            AppointmentRepository appointmentRepository,
            PasswordEncoder passwordEncoder,
            AesEncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.recordRepository = recordRepository;
        this.permissionRepository = permissionRepository;
        this.mediaRepository = mediaRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
    }

    @Override
    public void run(String... args) {
        try {
            seedUsers();
        } catch (Exception e) {
            log.warn("Database seeding encountered an issue (may already be seeded): {}", e.getMessage());
        }
    }

    private void seedUsers() {
        if (userRepository.findByEmail("admin.caredigital@gmail.com").isEmpty()) {
            createUser("admin", "admin.caredigital@gmail.com", "Admin@1234", "System", "Administrator", Role.ADMIN, "+1-555-0100");
        }
        if (userRepository.findByEmail("researcher@ecare.com").isEmpty()) {
            createUser("res_alex", "researcher@ecare.com", "Researcher@1234", "Dr. Alex", "Rivera", Role.RESEARCHER,
                    "+1-555-0104");
        }
        if (userRepository.findByEmail("insurance@ecare.com").isEmpty()) {
            createUser("ins_claire", "insurance@ecare.com", "Insurance@1234", "Claire", "Underwood", Role.INSURANCE,
                    "+1-555-0105");
        }
        // New Hospital Roles
        if (userRepository.findByEmail("recruiter@ecare.com").isEmpty()) {
            createUser("recruiter_elena", "recruiter@ecare.com", "Recruiter@1234", "Elena", "Vance",
                    Role.HEAD_RECRUITER, "+1-555-0106");
        }

        log.info("✓ System users for all  hospital roles verified / seeded");
    }

    private User createUser(String username, String email, String rawPassword, String first, String last, Role role,
            String phone) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(first);
        user.setLastName(last);
        user.setRole(role);
        user.setPhoneNumber(phone);
        user.setActive(true);
        user.setMfaEnabled(false);
        return userRepository.save(user);
    }

    private void createMedia(
            String patientId,
            String uploaderId,
            String type,
            String title,
            String desc,
            String fileName,
            String fileType,
            Long size,
            String data) {

        PatientMedia m = new PatientMedia();
        m.setPatientId(patientId);
        m.setUploaderId(uploaderId);
        m.setMediaType(type);
        m.setTitle(title);
        m.setDescription(desc);
        m.setFileName(fileName);
        m.setFileType(fileType);
        m.setFileSize(size);
        m.setFileData(data);
        m.setMediaHash("MEDIA_HASH_" + System.currentTimeMillis());
        mediaRepository.save(m);
    }

    private void createEncryptedRecord(
            String patientId,
            String doctorId,
            String recordType,
            String diagnosis,
            String treatment,
            String prescriptions,
            String notes,
            String history) {

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(patientId);
        record.setDoctorId(doctorId);
        record.setRecordType(recordType);
        record.setRecordDate(LocalDateTime.now().minusDays(3));

        // Encrypt with AES-256-GCM
        record.setEncryptedDiagnosis(encryptionService.encryptField(diagnosis));
        record.setEncryptedTreatment(encryptionService.encryptField(treatment));
        record.setEncryptedPrescriptions(encryptionService.encryptField(prescriptions));
        record.setEncryptedClinicalNotes(encryptionService.encryptField(notes));
        record.setEncryptedMedicalHistory(encryptionService.encryptField(history));

        record.setKeyVersion("v1");
        record.setEncryptionIv("GCM_INLINE");
        record.setEncryptionAuthTag("GCM_INLINE");
        record.setRecordHash("SEED_RECORD_" + System.currentTimeMillis());

        recordRepository.save(record);
    }
}
