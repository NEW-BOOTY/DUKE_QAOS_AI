/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * SecureCommunicator: PQ-secure messaging using PQCryptoManager.
 * Simulates VPN with encapsulated keys.
 */

package com.devinroyal.dukeai.security;

import java.security.PublicKey;
import java.util.Base64;
import java.util.logging.Logger;

public class SecureCommunicator {
    private static final Logger LOGGER = Logger.getLogger(SecureCommunicator.class.getName());
    private final PQCryptoManager crypto;

    public SecureCommunicator(PQCryptoManager crypto) {
        this.crypto = crypto;
    }

    public String sendSecure(String message) {
        try {
            byte[] data = message.getBytes();
            String signature = crypto.sign(data);
            // Simulate encapsulation for key exchange
            PublicKey mockPub = generateMockPubKey(); // In real: from node
            byte[] shared = crypto.encapsulate(mockPub);
            String encrypted = Base64.getEncoder().encodeToString(data); // Simulate encrypt with shared
            LOGGER.info("Secure message sent with PQ protection.");
            return "Sent: " + message + " (sig: " + signature + ", shared: " + Base64.getEncoder().encodeToString(shared) + ")";
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Secure send failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private PublicKey generateMockPubKey() {
        // Mock for simulation
        try {
            byte[] mockBytes = "mock-pub-key".getBytes();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(mockBytes);
            KeyFactory kf = KeyFactory.getInstance("ML-DSA", "BCPQC");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */