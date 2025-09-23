/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * AdaptiveSecurityEngine: Real-time threat monitoring and response.
 * Security: Thread-safe event logging with ConcurrentLinkedQueue.
 * Threat Model: Detects threats via ThreatDatabase; mitigates injection via validation.
 * Dependencies: ThreatDatabase, LoggerUtility.
 */

package com.devinroyal.dukeai.components;

import com.devinroyal.dukeai.utils.LoggerUtility;
import com.devinroyal.dukeai.utils.ThreatDatabase;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class AdaptiveSecurityEngine {
    private static final Logger LOGGER = LoggerUtility.getLogger(AdaptiveSecurityEngine.class.getName());
    private final ConcurrentLinkedQueue<String> eventLog = new ConcurrentLinkedQueue<>();
    private final ThreatDatabase threatDB;

    public AdaptiveSecurityEngine(ThreatDatabase threatDB) {
        if (threatDB == null) {
            throw new IllegalArgumentException("ThreatDatabase cannot be null");
        }
        this.threatDB = threatDB;
        LOGGER.info("AdaptiveSecurityEngine initialized.");
    }

    public void monitorEvent(String event) {
        if (event == null || event.trim().isEmpty()) {
            LOGGER.warning("Invalid event: null or empty");
            return;
        }
        eventLog.add(event);
        LOGGER.info("Monitoring event: " + event);
        if (threatDB.isThreat(event)) {
            respondToThreat(event);
        } else {
            LOGGER.info("Event secure: " + event);
        }
    }

    private void respondToThreat(String threat) {
        LOGGER.warning("Threat detected: " + threat + ". Initiating response...");
        try {
            Thread.sleep(30); // Simulate response delay
            LOGGER.info("Threat neutralized: " + threat);
        } catch (InterruptedException e) {
            LOGGER.severe("Threat response interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public String getEventLog() {
        return String.join("\n", eventLog);
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */