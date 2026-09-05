package com.backend.protection.encryption;

public interface EncryptionService {
    EncryptionResult encrypt(String plaintext);
    String decrypt(String ciphertext, String iv, String keyVersion);
    String encryptField(String plaintext);
    String decryptField(String ciphertext, String iv, String keyVersion);
}
