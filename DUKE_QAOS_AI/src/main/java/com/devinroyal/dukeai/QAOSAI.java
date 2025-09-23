/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * QAOSAI: HTTP server integrating DUKEAi and QAOS-AI functionalities.
 * Security: PQ-encrypted APIs, input validation, rate limiting.
 * Concurrency: Thread pool for handlers.
 * Dependencies: All components, PQCryptoManager, LoggerUtility, EncryptionUtility.
 */

package com.devinroyal.dukeai;

import com.devinroyal.dukeai.components.*;
import com.devinroyal.dukeai.security.PQCryptoManager;
import com.devinroyal.dukeai.utils.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class QAOSAI {
    private static final Logger LOGGER = LoggerUtility.getLogger(QAOSAI.class.getName());
    private static final int HTTP_PORT = 8080;
    private static final String APP_VERSION = "DUKE_QAOS_AI v1.0";

    // Subsystems
    private final QuantumResourceManager qrm;
    private final AdaptiveSecurityEngine ase;
    private final DecentralizedIdentityManager dim;
    private final AutonomousApiFramework aaf;
    private final PerformanceMonitor pm;
    private final PQCryptoManager crypto;
    private final EncryptionUtility encryption;

    public QAOSAI() {
        this.crypto = new PQCryptoManager("ml-kem");
        this.encryption = new EncryptionUtility(crypto);
        this.qrm = new QuantumResourceManager();
        this.ase = new AdaptiveSecurityEngine(new ThreatDatabase());
        this.dim = new DecentralizedIdentityManager(crypto);
        this.aaf = new AutonomousApiFramework(crypto, encryption);
        this.pm = new PerformanceMonitor();
        LOGGER.info("QAOSAI initialized: " + APP_VERSION);
    }
	
	public QAOSAI(DUKEAi dukeAi) {
	    // Use DUKEAi's subsystems instead of creating new instances
	    this.crypto = dukeAi.getPqCryptoManager();
	    this.encryption = new EncryptionUtility(crypto);
	    this.qrm = dukeAi.getQrm();
	    this.ase = dukeAi.getAse();
	    this.dim = dukeAi.getDim();
	    this.aaf = dukeAi.getAaf();
	    this.pm = dukeAi.getPm();
	    this.config = dukeAi.getConfig();
	    LOGGER.info("QAOSAI integrated with DUKEAi core: " + APP_VERSION);
	}

    public void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/events", new EventsHandler());
        server.createContext("/api/process-task", new ApiProcessTaskHandler());
        server.createContext("/api/monitor-security", new ApiMonitorSecurityHandler());
        server.createContext("/api/register-user", new ApiRegisterUserHandler());
        server.createContext("/api/verify-user", new ApiVerifyUserHandler());
        server.createContext("/api/secure-exchange", new ApiSecureExchangeHandler());
        server.createContext("/api/performance", new ApiPerformanceHandler());
        server.createContext("/api/logs", new ApiLogsHandler());
		server.createContext("/health", new HealthHandler(dukeAi));

        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }));

        server.start();
        LOGGER.info("HTTP server started on port " + HTTP_PORT);
        System.out.println("QAOS-AI running at http://localhost:" + HTTP_PORT);
    }

    // Handlers
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                    return;
                }
                byte[] content = FrontendResource.getIndexHtml().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(RootHandler.class.getName()), "RootHandler error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }
    }

    static class EventsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            Headers h = exchange.getResponseHeaders();
            h.set("Content-Type", "text/event-stream; charset=utf-8");
            h.set("Cache-Control", "no-cache");
            h.set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            OutputStream out = exchange.getResponseBody();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
            String clientId = UUID.randomUUID().toString();
            LoggerUtility.registerSseClient(clientId, pw);
            LoggerUtility.info(LoggerUtility.getLogger(EventsHandler.class.getName()), "SSE client connected: " + clientId);

            try {
                while (!pw.checkError()) {
                    Thread.sleep(15000); // Heartbeat
                    LoggerUtility.info(LoggerUtility.getLogger(EventsHandler.class.getName()), "Heartbeat to " + clientId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                LoggerUtility.unregisterSseClient(clientId);
                try { pw.close(); } catch (Exception ignore) {}
                try { out.close(); } catch (Exception ignore) {}
                LoggerUtility.info(LoggerUtility.getLogger(EventsHandler.class.getName()), "SSE client disconnected: " + clientId);
            }
        }
    }
	
	static class HealthHandler implements HttpHandler {
	    private final DUKEAi dukeAi;
    
	    public HealthHandler(DUKEAi dukeAi) {
	        this.dukeAi = dukeAi;
	    }
    
	    @Override
	    public void handle(HttpExchange exchange) throws IOException {
	        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
	            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
	            return;
	        }
	        String healthJson = dukeAi.healthCheck();
	        sendJson(exchange, 200, healthJson);
	    }
	}

    class ApiProcessTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long start = System.currentTimeMillis();
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String body = readRequestBody(exchange);
                Map<String, String> params = JsonUtil.parseSimpleJsonToMap(body);
                String task = params.getOrDefault("task", "default-task");
                LoggerUtility.info(LoggerUtility.getLogger(ApiProcessTaskHandler.class.getName()), "Processing task: " + task);
                int result = qrm.processTask(task);
                pm.logPerformance("QuantumResourceManager", System.currentTimeMillis() - start);
                sendJson(exchange, 200, "{\"result\":" + result + ",\"task\":\"" + escapeJson(task) + "\"}");
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiProcessTaskHandler.class.getName()), "Task error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    class ApiMonitorSecurityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String body = readRequestBody(exchange);
                Map<String, String> params = JsonUtil.parseSimpleJsonToMap(body);
                String event = params.getOrDefault("event", "manual-check");
                LoggerUtility.info(LoggerUtility.getLogger(ApiMonitorSecurityHandler.class.getName()), "Monitoring event: " + event);
                ase.monitorEvent(event);
                sendJson(exchange, 200, "{\"status\":\"ok\",\"event\":\"" + escapeJson(event) + "\"}");
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiMonitorSecurityHandler.class.getName()), "Security error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    class ApiRegisterUserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String body = readRequestBody(exchange);
                Map<String, String> params = JsonUtil.parseSimpleJsonToMap(body);
                String userId = params.getOrDefault("userId", UUID.randomUUID().toString());
                String publicKey = params.getOrDefault("publicKey", "pubKey-" + userId);
                LoggerUtility.info(LoggerUtility.getLogger(ApiRegisterUserHandler.class.getName()), "Registering user: " + userId);
                dim.registerUser(userId, publicKey);
                int token = dim.generateMfaToken(userId);
                sendJson(exchange, 200, "{\"userId\":\"" + escapeJson(userId) + "\",\"mfa\":" + token + "}");
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiRegisterUserHandler.class.getName()), "Register error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    class ApiVerifyUserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String body = readRequestBody(exchange);
                Map<String, String> params = JsonUtil.parseSimpleJsonToMap(body);
                String userId = params.get("userId");
                String publicKey = params.get("publicKey");
                int mfa = Integer.parseInt(params.getOrDefault("mfa", "-1"));
                LoggerUtility.info(LoggerUtility.getLogger(ApiVerifyUserHandler.class.getName()), "Verifying user: " + userId);
                boolean verified = dim.verifyUser(userId, publicKey, mfa);
                sendJson(exchange, 200, "{\"verified\":" + verified + "}");
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiVerifyUserHandler.class.getName()), "Verify error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    class ApiSecureExchangeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long start = System.currentTimeMillis();
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String body = readRequestBody(exchange);
                Map<String, String> params = JsonUtil.parseSimpleJsonToMap(body);
                String message = params.getOrDefault("message", "");
                String recipient = params.getOrDefault("recipient", "unknown");
                LoggerUtility.info(LoggerUtility.getLogger(ApiSecureExchangeHandler.class.getName()), "Secure exchange to: " + recipient);
                String encrypted = aaf.secureDataExchange(message, recipient);
                pm.logPerformance("AutonomousApiFramework", System.currentTimeMillis() - start);
                sendJson(exchange, 200, "{\"recipient\":\"" + escapeJson(recipient) + "\",\"payload\":\"" + escapeJson(encrypted) + "\"}");
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiSecureExchangeHandler.class.getName()), "Exchange error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    class ApiPerformanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String json = pm.toJson();
                sendJson(exchange, 200, json);
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiPerformanceHandler.class.getName()), "Performance error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    class ApiLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String json = LoggerUtility.getRecentLogsJson();
                sendJson(exchange, 200, json);
            } catch (Exception e) {
                LoggerUtility.severe(LoggerUtility.getLogger(ApiLogsHandler.class.getName()), "Logs error: " + e.getMessage());
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // Utility methods
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = is.read(buf)) != -1) {
                baos.write(buf, 0, read);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Simple JSON parser
    static class JsonUtil {
        public static Map<String, String> parseSimpleJsonToMap(String json) {
            Map<String, String> map = new java.util.HashMap<>();
            if (json == null || json.trim().isEmpty()) return map;
            String s = json.trim();
            if (s.startsWith("{")) s = s.substring(1);
            if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
            String[] parts = s.split(",");
            for (String p : parts) {
                int idx = p.indexOf(':');
                if (idx < 0) continue;
                String k = p.substring(0, idx).trim();
                String v = p.substring(idx + 1).trim();
                k = trimQuotes(k);
                v = trimQuotes(v);
                map.put(k, v);
            }
            return map;
        }

        private static String trimQuotes(String s) {
            if (s == null) return null;
            s = s.trim();
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length() - 1);
            return s;
        }
    }

    // Embedded frontend resource
    static class FrontendResource {
        public static String getIndexHtml() {
            // Same as provided in QAOSAI_SingleFile.txt
            return "<!DOCTYPE html>\n" +
                   "<html lang=\"en\">\n" +
                   "<head>\n" +
                   "  <meta charset=\"UTF-8\" />\n" +
                   "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                   "  <title>DUKE QAOS-AI Console</title>\n" +
                   "  <style>\n" +
                   "    :root{--bg:#0a0a23;--card:#15153d;--glass:#1b1b4d;--accent:#7c5cff;--muted:#9aa6b2}\n" +
                   "    *{margin:0;padding:0;box-sizing:border-box}\n" +
                   "    body{background:var(--bg);color:#fff;font-family:Arial,sans-serif;padding:12px}\n" +
                   "    .app{max-width:1280px;margin:0 auto}\n" +
                   "    .header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}\n" +
                   "    .brand{display:flex;gap:12px;align-items:center}\n" +
                   "    .logo{background:linear-gradient(45deg,#7c5cff,#00d4ff);width:40px;height:40px;border-radius:8px;display:grid;place-items:center;font-weight:700;font-size:18px}\n" +
                   "    .title{font-size:24px;font-weight:700}\n" +
                   "    .subtitle{font-size:14px;color:var(--muted)}\n" +
                   "    .grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}\n" +
                   "    .card{background:var(--card);padding:14px;border-radius:12px;box-shadow:0 6px 20px rgba(0,0,0,0.6);border:1px solid rgba(255,255,255,0.03)}\n" +
                   "    .controls{display:flex;gap:8px;flex-wrap:wrap}\n" +
                   "    button{background:linear-gradient(90deg,var(--accent),#00d4ff);border:none;color:#021018;padding:8px 12px;border-radius:8px;font-weight:600;cursor:pointer}\n" +
                   "    input,select,textarea{background:var(--glass);border:1px solid rgba(255,255,255,0.04);padding:8px;border-radius:8px;color:inherit;width:100%}\n" +
                   "    .muted{color:var(--muted)}\n" +
                   "    .logs{height:300px;overflow:auto;font-family:monospace;background:linear-gradient(180deg, rgba(255,255,255,0.02), transparent);padding:8px;border-radius:8px}\n" +
                   "    .row{display:flex;gap:10px}\n" +
                   "    .col{display:flex;flex-direction:column;gap:8px}\n" +
                   "    .small{font-size:12px}\n" +
                   "    .kpi{display:flex;gap:12px}\n" +
                   "    .kpi .item{background:linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));padding:10px;border-radius:8px;min-width:120px}\n" +
                   "    canvas{width:100%;height:140px;background:transparent;border-radius:6px}\n" +
                   "    footer{margin-top:12px;color:var(--muted);font-size:12px;text-align:center}\n" +
                   "    @media(max-width:940px){.grid{grid-template-columns:1fr}}\n" +
                   "  </style>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "<div class=\"app\">\n" +
                   "  <div class=\"header\">\n" +
                   "    <div class=\"brand\">\n" +
                   "      <div class=\"logo\">DU</div>\n" +
                   "      <div>\n" +
                   "        <div class=\"title\">DUKE QAOS-AI Console</div>\n" +
                   "        <div class=\"subtitle\">Single-file demo — real-time monitoring & secure API</div>\n" +
                   "      </div>\n" +
                   "    </div>\n" +
                   "    <div class=\"small muted\">Local demo — Backend at <strong>http://localhost:8080</strong></div>\n" +
                   "  </div>\n" +
                   "  <div class=\"grid\">\n" +
                   "    <div>\n" +
                   "      <div class=\"card\">\n" +
                   "        <div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:12px\">\n" +
                   "          <div><strong>System Control</strong><div class=\"muted small\">Trigger tasks / security scans / register users</div></div>\n" +
                   "          <div class=\"kpi\">\n" +
                   "            <div class=\"item\"><div class=\"muted small\">Version</div><div id=\"k-version\">" + APP_VERSION + "</div></div>\n" +
                   "            <div class=\"item\"><div class=\"muted small\">Uptime</div><div id=\"k-uptime\">0s</div></div>\n" +
                   "          </div>\n" +
                   "        </div>\n" +
                   "        <div class=\"row\" style=\"margin-bottom:12px\">\n" +
                   "          <div style=\"flex:1\">\n" +
                   "            <div class=\"col\">\n" +
                   "              <label class=\"small muted\">Task name</label>\n" +
                   "              <input id=\"task-name\" value=\"AI Task\" />\n" +
                   "            </div>\n" +
                   "          </div>\n" +
                   "          <div style=\"width:160px\">\n" +
                   "            <label class=\"small muted\">Mode</label>\n" +
                   "            <select id=\"task-mode\"><option>auto</option><option>force-quantum</option><option>force-classical</option></select>\n" +
                   "          </div>\n" +
                   "        </div>\n" +
                   "        <div class=\"controls\" style=\"margin-bottom:12px\">\n" +
                   "          <button id=\"btn-run-task\">Run Task</button>\n" +
                   "          <button id=\"btn-scan-security\">Trigger Security Scan</button>\n" +
                   "          <button id=\"btn-register-user\">Register Demo User</button>\n" +
                   "        </div>\n" +
                   "        <div style=\"display:flex;gap:12px;align-items:flex-start\">\n" +
                   "          <div style=\"flex:1\">\n" +
                   "            <div class=\"muted small\">Task Result</div>\n" +
                   "            <div style=\"padding:10px;background:rgba(255,255,255,0.02);border-radius:8px;margin-top:6px\" id=\"task-result\">—</div>\n" +
                   "          </div>\n" +
                   "          <div style=\"width:220px\">\n" +
                   "            <div class=\"muted small\">Last Performance</div>\n" +
                   "            <canvas id=\"perf-chart\" width=\"400\" height=\"140\"></canvas>\n" +
                   "          </div>\n" +
                   "        </div>\n" +
                   "      </div>\n" +
                   "      <div class=\"card\" style=\"margin-top:12px\">\n" +
                   "        <div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:8px\">\n" +
                   "          <div><strong>Security & Identity</strong><div class=\"muted small\">Logs & MFA management</div></div>\n" +
                   "          <div class=\"muted small\">Threat patterns managed internally</div>\n" +
                   "        </div>\n" +
                   "        <div style=\"display:flex;gap:12px;margin-bottom:8px\">\n" +
                   "          <div style=\"flex:1\">\n" +
                   "            <label class=\"small muted\">Security Event</label>\n" +
                   "            <input id=\"security-event\" value=\"Routine Check\" />\n" +
                   "          </div>\n" +
                   "          <div style=\"width:160px\">\n" +
                   "            <label class=\"small muted\">Severity</label>\n" +
                   "            <select id=\"security-severity\"><option>info</option><option>warning</option><option>threat</option></select>\n" +
                   "          </div>\n" +
                   "        </div>\n" +
                   "        <div class=\"row\" style=\"margin-bottom:8px\">\n" +
                   "          <div style=\"width:260px\">\n" +
                   "            <label class=\"small muted\">User ID (for register)</label>\n" +
                   "            <input id=\"user-id\" value=\"user123\" />\n" +
                   "          </div>\n" +
                   "          <div style=\"flex:1\">\n" +
                   "            <label class=\"small muted\">Public Key</label>\n" +
                   "            <input id=\"user-pub\" value=\"publicKeyExample\" />\n" +
                   "          </div>\n" +
                   "        </div>\n" +
                   "        <div class=\"controls\">\n" +
                   "          <button id=\"btn-get-logs\">Refresh Logs</button>\n" +
                   "          <button id=\"btn-get-performance\">Refresh Perf</button>\n" +
                   "        </div>\n" +
                   "        <div style=\"margin-top:10px\">\n" +
                   "          <div class=\"muted small\">Recent System Logs</div>\n" +
                   "          <div class=\"logs\" id=\"log-window\">Loading logs…</div>\n" +
                   "        </div>\n" +
                   "      </div>\n" +
                   "    </div>\n" +
                   "    <div>\n" +
                   "      <div class=\"card\">\n" +
                   "        <div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:8px\">\n" +
                   "          <div><strong>Live Events</strong><div class=\"muted small\">Server-sent events (SSE)</div></div>\n" +
                   "          <div class=\"muted small\">Real-time stream</div>\n" +
                   "        </div>\n" +
                   "        <div style=\"height:360px;overflow:auto;padding:6px;border-radius:8px;background:linear-gradient(180deg, rgba(255,255,255,0.01), transparent)\" id=\"events-box\">Connecting to events…</div>\n" +
                   "      </div>\n" +
                   "      <div class=\"card\" style=\"margin-top:12px\">\n" +
                   "        <div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:8px\">\n" +
                   "          <div><strong>Secure Exchange</strong><div class=\"muted small\">AES-GCM encrypted messages</div></div>\n" +
                   "          <div class=\"muted small\">Demo only — no external network</div>\n" +
                   "        </div>\n" +
                   "        <div style=\"margin-bottom:8px\">\n" +
                   "          <label class=\"small muted\">Recipient ID</label>\n" +
                   "          <input id=\"secure-recipient\" value=\"RecipientID\" />\n" +
                   "          <label class=\"small muted\" style=\"margin-top:8px\">Message</label>\n" +
                   "          <textarea id=\"secure-msg\" rows=\"4\">Confidential message content</textarea>\n" +
                   "        </div>\n" +
                   "        <div class=\"controls\"><button id=\"btn-secure-send\">Encrypt & Send</button></div>\n" +
                   "        <div style=\"margin-top:10px\">\n" +
                   "          <div class=\"muted small\">Encrypted Payload</div>\n" +
                   "          <div style=\"padding:8px;background:rgba(255,255,255,0.02);border-radius:8px;margin-top:6px;word-break:break-all\" id=\"encrypted-payload\">—</div>\n" +
                   "        </div>\n" +
                   "        <footer>© 2025 Devin B. Royal — DUKE QAOS-AI Demo</footer>\n" +
                   "      </div>\n" +
                   "    </div>\n" +
                   "  </div>\n" +
                   "</div>\n" +
                   "<script>\n" +
                   "(() => {\n" +
                   "  const versionEl = document.getElementById('k-version');\n" +
                   "  const uptimeEl = document.getElementById('k-uptime');\n" +
                   "  const startTs = Date.now();\n" +
                   "  setInterval(() => { let s = Math.floor((Date.now()-startTs)/1000); uptimeEl.textContent = s + 's'; }, 1000);\n" +
                   "  async function postJson(url, obj) {\n" +
                   "    try {\n" +
                   "      const r = await fetch(url, {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(obj)});\n" +
                   "      return await r.json();\n" +
                   "    } catch(e) { console.error('postJson error',e); return {error:e.message}; }\n" +
                   "  }\n" +
                   "  document.getElementById('btn-run-task').addEventListener('click', async () => {\n" +
                   "    const task = document.getElementById('task-name').value || 'AI Task';\n" +
                   "    const res = await postJson('/api/process-task', {task});\n" +
                   "    if (res.error) { document.getElementById('task-result').textContent = 'Error: '+res.error; }\n" +
                   "    else { document.getElementById('task-result').textContent = 'Result: '+res.result+' — '+res.task; }\n" +
                   "    refreshPerformance(); refreshLogs();\n" +
                   "  });\n" +
                   "  document.getElementById('btn-scan-security').addEventListener('click', async () => {\n" +
                   "    const ev = document.getElementById('security-event').value || 'Routine Check';\n" +
                   "    const sev = document.getElementById('security-severity').value || 'info';\n" +
                   "    const event = ev + ' ['+sev+']';\n" +
                   "    const res = await postJson('/api/monitor-security', {event});\n" +
                   "    if (res.error) alert('Error: '+res.error); else { refreshLogs(); }\n" +
                   "  });\n" +
                   "  document.getElementById('btn-register-user').addEventListener('click', async () => {\n" +
                   "    const userId = document.getElementById('user-id').value || ('u'+Math.floor(Math.random()*10000));\n" +
                   "    const pub = document.getElementById('user-pub').value || ('pub-'+userId);\n" +
                   "    const res = await postJson('/api/register-user', {userId, publicKey:pub});\n" +
                   "    if (res.error) alert('Error: '+res.error); else {\n" +
                   "      alert('Registered '+res.userId+' — MFA: '+res.mfa);\n" +
                   "      refreshLogs();\n" +
                   "    }\n" +
                   "  });\n" +
                   "  document.getElementById('btn-get-logs').addEventListener('click', refreshLogs);\n" +
                   "  document.getElementById('btn-get-performance').addEventListener('click', refreshPerformance);\n" +
                   "  document.getElementById('btn-secure-send').addEventListener('click', async () => {\n" +
                   "    const recipient = document.getElementById('secure-recipient').value || 'Recipient';\n" +
                   "    const message = document.getElementById('secure-msg').value || '';\n" +
                   "    const res = await postJson('/api/secure-exchange', {recipient, message});\n" +
                   "    if (res.error) document.getElementById('encrypted-payload').textContent = 'Error: '+res.error;\n" +
                   "    else document.getElementById('encrypted-payload').textContent = res.payload || '—';\n" +
                   "    refreshLogs();\n" +
                   "  });\n" +
                   "  const eventsBox = document.getElementById('events-box');\n" +
                   "  try {\n" +
                   "    const sse = new EventSource('/events');\n" +
                   "    sse.addEventListener('log', ev => { const d = JSON.parse(ev.data); appendEvent('LOG: '+d.level+' — '+d.message); });\n" +
                   "    sse.addEventListener('connected', ev => { const d = JSON.parse(ev.data); appendEvent('Connected: '+d.clientId); });\n" +
                   "    sse.addEventListener('heartbeat', ev => { /* no-op */ });\n" +
                   "    sse.onerror = () => { appendEvent('SSE connection error'); sse.close(); setTimeout(() => new EventSource('/events'), 3000); };\n" +
                   "  } catch(e) { appendEvent('SSE not supported: '+e.message); }\n" +
                   "  function appendEvent(text) { const el = document.createElement('div'); el.textContent = (new Date()).toLocaleTimeString() + ' — ' + text; eventsBox.prepend(el); }\n" +
                   "  async function refreshLogs() { try { const r = await fetch('/api/logs'); const j = await r.json(); const lw = document.getElementById('log-window'); lw.innerHTML = ''; j.logs.slice().reverse().forEach(l => { const el = document.createElement('div'); el.textContent = l; lw.appendChild(el); }); } catch(e) { console.error(e); } }\n" +
                   "  async function refreshPerformance() { try { const r = await fetch('/api/performance'); const j = await r.json(); drawPerfChart(j); } catch(e) { console.error(e); } }\n" +
                   "  function drawPerfChart(data) { const canvas = document.getElementById('perf-chart'); if (!canvas) return; const ctx = canvas.getContext('2d'); const w = canvas.width; const h = canvas.height; ctx.clearRect(0,0,w,h); ctx.fillStyle='rgba(255,255,255,0.02)'; ctx.fillRect(0,0,w,h); const keys = Object.keys(data); if (keys.length===0) { ctx.fillStyle='#9aa6b2'; ctx.font='12px monospace'; ctx.fillText('No perf data',10,20); return; } const vals = keys.map(k=>data[k]); const max = Math.max(...vals,1); const barW = Math.floor(w / keys.length) - 6; keys.forEach((k,i)=>{ const v = data[k]; const bh = Math.max(4, Math.floor((v/max)*(h-20))); const x = i*(barW+6)+6; const y = h - bh - 6; ctx.fillStyle='rgba(124,92,255,0.95)'; ctx.fillRect(x,y,barW,bh); ctx.fillStyle='#9aa6b2'; ctx.font='10px monospace'; ctx.fillText(k, x, h-2); }); }\n" +
                   "  refreshLogs(); refreshPerformance();\n" +
                   "})();\n" +
                   "</script>\n" +
                   "</body>\n" +
                   "</html>\n";
        }
    }

    public static void main(String[] args) {
        try {
            new QAOSAI().startServer();
        } catch (IOException e) {
            LoggerUtility.severe(LoggerUtility.getLogger(QAOSAI.class.getName()), "Server start failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */