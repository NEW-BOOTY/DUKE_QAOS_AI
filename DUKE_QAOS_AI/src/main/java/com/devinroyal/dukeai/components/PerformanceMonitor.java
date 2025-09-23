/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * PerformanceMonitor: Logs execution times for system components.
 * Concurrency: Thread-safe with ConcurrentHashMap.
 * Dependencies: LoggerUtility.
 */

package com.devinroyal.dukeai.components;

import com.devinroyal.dukeai.utils.LoggerUtility;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PerformanceMonitor {
    private static final Logger LOGGER = LoggerUtility.getLogger(PerformanceMonitor.class.getName());
    private final ConcurrentHashMap<String, Long> performanceLog = new ConcurrentHashMap<>();

    public void logPerformance(String component, long timeTaken) {
        if (component == null || component.trim().isEmpty() || timeTaken < 0) {
            LOGGER.warning("Invalid performance log input");
            return;
        }
        performanceLog.put(component, timeTaken);
        LOGGER.info(component + " execution time: " + timeTaken + "ms");
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        performanceLog.forEach((key, value) ->
            json.append("\"").append(key).append("\":").append(value).append(",")
        );
        if (!performanceLog.isEmpty()) {
            json.setLength(json.length() - 1); // Remove last comma
        }
        json.append("}");
        return json.toString();
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */