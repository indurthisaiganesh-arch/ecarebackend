package com.backend.protection.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class AesEncryptionServiceTest {

    private AesEncryptionService encryptionService;
    // 256-bit test key (64 hex characters)
    private final String testKeyHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        encryptionService = new AesEncryptionService();
        ReflectionTestUtils.setField(encryptionService, "masterKeyHex", testKeyHex);
        ReflectionTestUtils.setField(encryptionService, "currentKeyVersion", "v1");
    }

    @Test
    void testEncryptAndDecrypt() {
        String sensitiveData = "Patient diagnosed with Acute Myocardial Infarction; BP: 140/90";
        EncryptionResult result = encryptionService.encrypt(sensitiveData);

        assertNotNull(result);
        assertNotNull(result.getCiphertext());
        assertNotNull(result.getIv());
        assertEquals("v1", result.getKeyVersion());
        assertNotEquals(sensitiveData, result.getCiphertext());

        String decrypted = encryptionService.decrypt(result.getCiphertext(), result.getIv(), result.getKeyVersion());
        assertEquals(sensitiveData, decrypted);
    }

    @Test
    void testEncryptFieldCombinedFormat() {
        String clinicalNotes = "Patient allergic to Penicillin. Prescribed Azithromycin 500mg daily.";
        String combined = encryptionService.encryptField(clinicalNotes);

        assertNotNull(combined);
        assertTrue(combined.contains("::"));
        String[] parts = combined.split("::");
        assertEquals(3, parts.length);

        String decrypted = encryptionService.decryptField(combined, null, null);
        assertEquals(clinicalNotes, decrypted);
    }

    @Test
    void testTamperedCiphertextFailsAuthentication() {
        String data = "Confidential Patient Medical Record";
        EncryptionResult result = encryptionService.encrypt(data);

        // Tamper with ciphertext by altering characters
        String tamperedCiphertext = "X" + result.getCiphertext().substring(1);

        assertThrows(Exception.class, () -> {
            encryptionService.decrypt(tamperedCiphertext, result.getIv(), result.getKeyVersion());
        });
    }
}
