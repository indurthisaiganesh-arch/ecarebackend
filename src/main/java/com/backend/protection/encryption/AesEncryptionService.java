package com.backend.protection.encryption;

import com.backend.protection.exception.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AES-256-GCM authenticated encryption service.
 * Uses a unique 96-bit nonce (IV) per encryption operation to ensure semantic security.
 * AES-GCM provides confidentiality + integrity (authenticated encryption).
 */
@Service
public class AesEncryptionService implements EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12; // 96 bits
    private static final int TAG_LENGTH_BITS = 128; // 128-bit auth tag
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.encryption.master-key}")
    private String masterKeyHex;

    @Value("${app.encryption.current-key-version:v1}")
    private String currentKeyVersion;

    // Key version -> SecretKey mapping (supports key rotation)
    private final Map<String, SecretKey> keyCache = new ConcurrentHashMap<>();

    private SecretKey getKey(String version) {
        return keyCache.computeIfAbsent(version, v -> {
            // In a production system, each version would have its own key from KMS
            // For development, we derive from master key with version tag
            byte[] hexBytes = hexToBytes(masterKeyHex);
            if (hexBytes.length != 32) {
                throw new EncryptionException("AES-256 key must be exactly 256 bits (32 bytes / 64 hex chars)");
            }
            return new SecretKeySpec(hexBytes, "AES");
        });
    }

    @Override
    public EncryptionResult encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv); // cryptographically random IV per operation

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(currentKeyVersion), new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertextWithTag = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // GCM appends auth tag at end of ciphertext - store as Base64
            String ciphertextB64 = Base64.getEncoder().encodeToString(ciphertextWithTag);
            String ivB64 = Base64.getEncoder().encodeToString(iv);

            // Auth tag is included in ciphertextWithTag (last 16 bytes for TAG_LENGTH_BITS=128)
            return new EncryptionResult(ciphertextB64, ivB64, "GCM_INTEGRATED", currentKeyVersion);
        } catch (Exception e) {
            log.error("Encryption failed");
            throw new EncryptionException("Failed to encrypt data: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String ciphertextB64, String ivB64, String keyVersion) {
        if (ciphertextB64 == null) return null;
        try {
            byte[] ciphertext = Base64.getDecoder().decode(ciphertextB64);
            byte[] iv = Base64.getDecoder().decode(ivB64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(keyVersion), new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            log.error("Decryption failed for key version {}", keyVersion);
            throw new EncryptionException("Failed to decrypt data - data may be tampered", e);
        }
    }

    @Override
    public String encryptField(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        EncryptionResult result = encrypt(plaintext);
        // Store as: base64(ciphertext):base64(iv):keyVersion
        return result.getCiphertext() + "::" + result.getIv() + "::" + result.getKeyVersion();
    }

    @Override
    public String decryptField(String encryptedField, String iv, String keyVersion) {
        if (encryptedField == null || encryptedField.isBlank()) return encryptedField;
        // If stored in combined format, parse it
        if (encryptedField.contains("::")) {
            String[] parts = encryptedField.split("::");
            if (parts.length == 3) {
                return decrypt(parts[0], parts[1], parts[2]);
            }
        }
        return decrypt(encryptedField, iv, keyVersion);
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
