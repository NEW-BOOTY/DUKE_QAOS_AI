/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * DecentralizedIdentityManager: Blockchain-like identity management with MFA.
 * Security: Validates inputs, uses PQ signatures for blockchain entries.
 * Concurrency: Thread-safe with ConcurrentHashMap.
 * Dependencies: PQCryptoManager, LoggerUtility.
 */

package com.devinroyal.dukeai.components;

import com.devinroyal.dukeai.security.PQCryptoManager;
import com.devinroyal.dukeai.utils.LoggerUtility;
import java.security.PublicKey;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DecentralizedIdentityManager {
    private static final Logger LOGGER = LoggerUtility.getLogger(DecentralizedIdentityManager.class.getName());
    private final ConcurrentHashMap<String, String> blockchain = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> mfaTokens = new ConcurrentHashMap<>();
    private final PQCryptoManager crypto;

    public DecentralizedIdentityManager(PQCryptoManager crypto) {
        if (crypto == null) {
            throw new IllegalArgumentException("PQCryptoManager cannot be null");
        }
        this.crypto = crypto;
        LOGGER.info("DecentralizedIdentityManager initialized.");
    }

    public void registerUser(String userId, String publicKey) {
        if (userId == null || userId.trim().isEmpty() || publicKey == null || publicKey.trim().isEmpty()) {
            LOGGER.warning("Invalid userId or publicKey");
            throw new IllegalArgumentException("UserId and publicKey cannot be null or empty");
        }
        String signedKey = crypto.sign(publicKey.getBytes());
        blockchain.put(userId, signedKey);
        LOGGER.info("Registered user: " + userId + " with signed key.");
        generateMfaToken(userId);
    }

    public int generateMfaToken(String userId) {
        if (!blockchain.containsKey(userId)) {
            LOGGER.warning("User not found: " + userId);
            throw new IllegalStateException("User not registered");
        }
        int token = 100000 + new Random().nextInt(900000); // 6-digit MFA
        mfaTokens.put(userId, token);
        LOGGER.info("MFA Token for " + userId + ": " + token);
        return token;
    }

    public boolean verifyUser(String userId, String publicKey, int mfaToken) {
        if (userId == null || publicKey == null) {
            LOGGER.warning("Invalid verification input");
            return false;
        }
        boolean keyValid = blockchain.containsKey(userId) && crypto.verify(publicKey.getBytes(), blockchain.get(userId), generateMockPubKey());
        boolean mfaValid = mfaTokens.getOrDefault(userId, -1) == mfaToken;
        boolean verified = keyValid && mfaValid;
        LOGGER.info("Verification for " + userId + ": " + (verified ? "Success" : "Failed"));
        return verified;
    }

    private PublicKey generateMockPubKey() {
        // Mock for simulation
        try {
            byte[] mockBytes = "mock-pub-key".getBytes();
            return new java.security.spec.X509EncodedKeySpec(mockBytes).getPublicKey();
        } catch (Exception e) {
            LOGGER.severe("Mock public key generation failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */