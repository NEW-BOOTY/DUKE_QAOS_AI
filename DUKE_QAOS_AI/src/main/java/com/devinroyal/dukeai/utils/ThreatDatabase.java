/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * ThreatDatabase: Simple in-memory threat pattern storage.
 * Security: Thread-safe with ConcurrentHashMap.
 * Dependencies: LoggerUtility.
 */

package com.devinroyal.dukeai.utils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ThreatDatabase {
    private static final Logger LOGGER = LoggerUtility.getLogger(ThreatDatabase.class.getName());
    private final Set<String> knownThreats = ConcurrentHashMap.newKeySet();

    public ThreatDatabase() {
        // Initialize with sample threats
        knownThreats.add("threat");
        knownThreats.add("attack");
        LOGGER.info("ThreatDatabase initialized with " + knownThreats.size() + " patterns.");
    }

    public boolean isThreat(String event) {
        if (event == null) {
            return false;
        }
        return knownThreats.stream().anyMatch(threat -> event.toLowerCase().contains(threat));
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */