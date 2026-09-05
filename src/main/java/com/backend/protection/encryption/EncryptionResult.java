package com.backend.protection.encryption;

public class EncryptionResult {
    private final String ciphertext;
    private final String iv;
    private final String authTag;
    private final String keyVersion;

    public EncryptionResult(String ciphertext, String iv, String authTag, String keyVersion) {
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.authTag = authTag;
        this.keyVersion = keyVersion;
    }

    public String getCiphertext() { return ciphertext; }
    public String getIv() { return iv; }
    public String getAuthTag() { return authTag; }
    public String getKeyVersion() { return keyVersion; }
}
