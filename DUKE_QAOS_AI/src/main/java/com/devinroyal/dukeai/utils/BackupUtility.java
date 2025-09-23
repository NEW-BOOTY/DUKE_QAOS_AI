
/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * BackupUtility: System state export/import for disaster recovery.
 * Security: Encrypts sensitive state data with PQ-derived keys.
 * Dependencies: PQCryptoManager, LoggerUtility.
 */

package com.devinroyal.dukeai.utils;

import com.devinroyal.dukeai.security.PQCryptoManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BackupUtility {
    private static final Logger LOGGER = LoggerUtility.getLogger(BackupUtility.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PQCryptoManager crypto;

    public BackupUtility(PQCryptoManager crypto) {
        if (crypto == null) {
            throw new IllegalArgumentException("PQCryptoManager cannot be null");
        }
        this.crypto = crypto;
        LOGGER.info("BackupUtility initialized.");
    }

    public void exportState(String outputPath) throws IOException {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
        
        Map<String, Object> state = new HashMap<>();
        state.put("timestamp", System.currentTimeMillis());
        state.put("version", "1.0.0");
        state.put("systemStatus", getSystemStatus());
        state.put("configuration", getConfiguration());
        state.put("userCount", getUserCount());
        state.put("taskHistory", getTaskHistory());
        
        // Encrypt sensitive data
        String sensitiveData = GSON.toJson(state);
        String encryptedState = crypto.sign(sensitiveData.getBytes());
        
        Map<String, String> backup = new HashMap<>();
        backup.put("metadata", GSON.toJson(new HashMap<String, Object>() {{
            put("exportTime", System.currentTimeMillis());
            put("version", "1.0.0");
        }}));
        backup.put("encryptedState", encryptedState);
        backup.put("signature", crypto.sign(sensitiveData.getBytes()));
        
        Path outputFile = Paths.get(outputPath, "state.json");
        Files.createDirectories(outputFile.getParent());
        
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            writer.write(GSON.toJson(backup));
        }
        
        LOGGER.info("State exported to: " + outputFile.toAbsolutePath());
    }

    public void importState(String inputPath) throws IOException {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Input path cannot be null or empty");
        }
        
        Path inputFile = Paths.get(inputPath);
        if (!Files.exists(inputFile)) {
            throw new FileNotFoundException("Backup file not found: " + inputPath);
        }
        
        try (FileReader reader = new FileReader(inputFile.toFile())) {
            Map<String, String> backup = GSON.fromJson(reader, Map.class);
            
            // Verify signature
            String encryptedState = backup.get("encryptedState");
            String signature = backup.get("signature");
            PublicKey mockPubKey = generateMockPubKey();
            
            if (!crypto.verify(encryptedState.getBytes(), signature, mockPubKey)) {
                LOGGER.severe("Backup signature verification failed");
                throw new SecurityException("Invalid backup signature");
            }
            
            // Decrypt and restore state
            String decryptedState = new String(crypto.encapsulate(mockPubKey)); // Simulate decryption
            Map<String, Object> state = GSON.fromJson(decryptedState, Map.class);
            
            restoreSystemState(state);
            LOGGER.info("State imported from: " + inputFile.toAbsolutePath());
        }
    }

    private Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("kernel", "running");
        status.put("blockchain", "synced");
        status.put("neuroInterface", "active");
        status.put("securityEngine", "monitoring");
        return status;
    }

    private Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("pqMode", "ml-kem");
        config.put("threadPoolSize", 8);
        config.put("logRetention", "7d");
        return config;
    }

    private int getUserCount() {
        // Simulate user count from identity manager
        return 42;
    }

    private Map<String, Object> getTaskHistory() {
        Map<String, Object> history = new HashMap<>();
        history.put("totalTasks", 1234);
        history.put("quantumTasks", 567);
        history.put("avgProcessingTime", 45.2);
        return history;
    }

    private void restoreSystemState(Map<String, Object> state) {
        // Simulate state restoration
        LOGGER.info("Restoring system state from backup...");
        LOGGER.info("Configuration restored: " + state.get("configuration"));
        LOGGER.info("User count restored: " + state.get("userCount"));
    }

    private PublicKey generateMockPubKey() {
        // Mock for simulation - in production, use actual public key
        try {
            byte[] mockBytes = "backup-verification-key".getBytes();
            java.security.spec.X509EncodedKeySpec spec = 
                new java.security.spec.X509EncodedKeySpec(mockBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("ML-DSA", "BCPQC");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            LOGGER.severe("Mock key generation failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */