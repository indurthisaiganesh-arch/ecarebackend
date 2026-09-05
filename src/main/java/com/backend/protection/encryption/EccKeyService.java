package com.backend.protection.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import javax.crypto.KeyAgreement;

/**
 * ECC Key Service using ECDH for key exchange/agreement.
 * Uses secp256r1 (NIST P-256) curve via Bouncy Castle provider.
 * ECC is used for secure key protection/exchange, NOT for bulk data encryption.
 * AES-256-GCM handles all bulk medical record encryption.
 */
@Service
public class EccKeyService {

    private static final Logger log = LoggerFactory.getLogger(EccKeyService.class);
    private static final String CURVE = "secp256r1";
    private static final String PROVIDER = "BC";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generate a new ECC key pair for key exchange.
     */
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", PROVIDER);
            kpg.initialize(new ECGenParameterSpec(CURVE), new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            log.error("Failed to generate ECC key pair");
            throw new RuntimeException("ECC key generation failed", e);
        }
    }

    /**
     * Perform ECDH key agreement between a private key and a public key.
     * Returns shared secret bytes for use as a key wrapping key or session key.
     */
    public byte[] performKeyAgreement(PrivateKey privateKey, PublicKey publicKey) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("ECDH", PROVIDER);
            ka.init(privateKey);
            ka.doPhase(publicKey, true);
            return ka.generateSecret();
        } catch (Exception e) {
            log.error("ECDH key agreement failed");
            throw new RuntimeException("ECDH key agreement failed", e);
        }
    }

    /**
     * Encode public key to Base64 for storage or transmission.
     */
    public String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Encode private key to Base64 for secure storage.
     */
    public String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}
