/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * LoggerUtility: Centralized logging with SSE support for real-time frontend updates.
 * Security: Sanitizes log messages to prevent injection.
 * Concurrency: Thread-safe with ConcurrentLinkedQueue.
 */

package com.devinroyal.dukeai.utils;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class LoggerUtility {
    private static final Map<String, PrintWriter> sseClients = new ConcurrentHashMap<>();
    private static final Queue<String> recentLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOGS = 1000;

    public static Logger getLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false); // Avoid default console logging
        return logger;
    }

    public static void info(Logger logger, String message) {
        String sanitized = sanitize(message);
        logger.info(sanitized);
        broadcast("INFO", sanitized);
    }

    public static void warning(Logger logger, String message) {
        String sanitized = sanitize(message);
        logger.warning(sanitized);
        broadcast("WARNING", sanitized);
    }

    public static void severe(Logger logger, String message) {
        String sanitized = sanitize(message);
        logger.severe(sanitized);
        broadcast("SEVERE", sanitized);
    }

    private static String sanitize(String message) {
        return message == null ? "" : message.replace("\n", "\\n").replace("\"", "\\\"");
    }

    public static void registerSseClient(String clientId, PrintWriter writer) {
        sseClients.put(clientId, writer);
    }

    public static void unregisterSseClient(String clientId) {
        sseClients.remove(clientId);
    }

    private static void broadcast(String level, String message) {
        String event = "data: {\"level\":\"" + level + "\",\"message\":\"" + message + "\"}\n\n";
        recentLogs.add(level + ": " + message);
        if (recentLogs.size() > MAX_LOGS) {
            recentLogs.poll();
        }
        sseClients.forEach((id, writer) -> {
            try {
                writer.write(event);
                writer.flush();
            } catch (Exception e) {
                unregisterSseClient(id);
            }
        });
    }

    public static String getRecentLogsJson() {
        StringBuilder json = new StringBuilder("{\"logs\":[");
        String[] logs = recentLogs.toArray(new String[0]);
        for (int i = 0; i < logs.length; i++) {
            json.append("\"").append(sanitize(logs[i])).append("\"");
            if (i < logs.length - 1) {
                json.append(",");
            }
        }
        json.append("]}");
        return json.toString();
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */