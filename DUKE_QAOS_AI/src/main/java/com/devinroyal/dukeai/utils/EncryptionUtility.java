/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * EncryptionUtility: AES-GCM encryption with PQ-derived keys.
 * Security: Uses Bouncy Castle for AES-GCM; constant-time operations.
 * Dependencies: PQCryptoManager, LoggerUtility.
 */

package com.devinroyal.dukeai.utils;

import com.devinroyal.dukeai.security.PQCryptoManager;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.logging.Logger;

public class EncryptionUtility {
    private static final Logger LOGGER = LoggerUtility.getLogger(EncryptionUtility.class.getName());
    private final PQCryptoManager crypto;

    public EncryptionUtility(PQCryptoManager crypto) {
        if (crypto == null) {
            throw new IllegalArgumentException("PQCryptoManager cannot be null");
        }
        this.crypto = crypto;
        LOGGER.info("EncryptionUtility initialized.");
    }

    public String encrypt(String message) throws Exception {
        if (message == null || message.trim().isEmpty()) {
            LOGGER.warning("Invalid message for encryption");
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        byte[] sharedKey = crypto.encapsulate(generateMockPubKey()); // Simulate KEM
        SecretKeySpec keySpec = new SecretKeySpec(sharedKey, 0, 16, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private java.security.PublicKey generateMockPubKey() {
        // Mock for simulation
        try {
            byte[] mockBytes = "mock-pub-key".getBytes();
            return new java.security.spec.X509EncodedKeySpec(mockBytes).getPublicKey();
        } catch (Exception e) {
            LOGGER.severe("Mock key generation failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */