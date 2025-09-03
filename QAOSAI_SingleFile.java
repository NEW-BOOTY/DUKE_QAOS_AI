/*
 * Copyright © 2024 Devin B. Royal.
 * All Rights Reserved.
 *
 * DUKE_QAOS_AI — Single-file, self-contained Java application
 * - Embedded HTTP server (com.sun.net.httpserver.HttpServer)
 * - Serves a fully styled, interactive single-page frontend
 * - Includes: QuantumResourceManager, AdaptiveSecurityEngine,
 *   DecentralizedIdentityManager, AutonomousApiFramework,
 *   PerformanceMonitor, LogService (SSE).
 *
 * Compile:
 *   javac QAOSAI_SingleFile.java
 * Run:
 *   java QAOSAI_SingleFile
 *
 * Open: http://localhost:8080
 *
 * Note: This program aims to be production-ready for demonstration purposes.
 *       Secure deployment should add TLS (reverse proxy like Nginx/Let's Encrypt),
 *       production logging sinks, secret management, and hardened JVM options.
 */

import com.sun.net.httpserver.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/* ----------------------
   Core Application
   ---------------------- */
public class QAOSAI_SingleFile {

    // Server configuration
    private static final int HTTP_PORT = 8080;
    private static final String APP_VERSION = "DUKE_QAOS_AI v1.0 (single-file)";
    private static final LogService LOG = new LogService(1000); // central log + SSE

    // Core subsystems (thread-safe)
    private static final QuantumResourceManager QRM = new QuantumResourceManager(LOG);
    private static final ThreatDatabase THREAT_DB = new ThreatDatabase(LOG);
    private static final AdaptiveSecurityEngine ASE = new AdaptiveSecurityEngine(THREAT_DB, LOG);
    private static final DecentralizedIdentityManager DIM = new DecentralizedIdentityManager(LOG);
    private static final AutonomousApiFramework AAF = new AutonomousApiFramework(LOG);
    private static final PerformanceMonitor PM = new PerformanceMonitor(LOG);

    public static void main(String[] args) {
        LOG.info("Starting " + APP_VERSION);
        try {
            startServer(HTTP_PORT);
        } catch (Exception e) {
            LOG.error("Fatal error starting server: " + e.getMessage(), e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void startServer(int port) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(port);
        HttpServer server = HttpServer.create(addr, 0);
        server.createContext("/", new RootHandler());
        server.createContext("/events", new EventsHandler());
        server.createContext("/api/process-task", new ApiProcessTaskHandler());
        server.createContext("/api/monitor-security", new ApiMonitorSecurityHandler());
        server.createContext("/api/register-user", new ApiRegisterUserHandler());
        server.createContext("/api/verify-user", new ApiVerifyUserHandler());
        server.createContext("/api/secure-exchange", new ApiSecureExchangeHandler());
        server.createContext("/api/performance", new ApiPerformanceHandler());
        server.createContext("/api/logs", new ApiLogsHandler());

        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }));

        server.start();
        LOG.info("HTTP server started on port " + port);
        System.out.println("QAOS-AI running at http://localhost:" + port);
    }

    /* ----------------------
       HTTP Handlers
       ---------------------- */

    // Serves the single-page application (embedded HTML/CSS/JS)
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
                LOG.error("RootHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"Internal Server Error\"}");
            }
        }
    }

    // SSE event stream for real-time logs/events
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
            LOG.registerSseClient(clientId, pw);
            LOG.info("SSE client connected: " + clientId);

            // Keep connection open until client disconnects
            try {
                // block until underlying stream closed by client
                while (!pw.checkError()) {
                    Thread.sleep(15000); // heartbeat interval (server side)
                    // send heartbeat
                    LOG.sendEventToClient(clientId, "heartbeat", "{\"time\":\"" + Instant.now().toString() + "\"}");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                LOG.unregisterSseClient(clientId);
                try { pw.close(); } catch (Exception ignore) {}
                try { out.close(); } catch (Exception ignore) {}
                LOG.info("SSE client disconnected: " + clientId);
            }
        }
    }

    // API: process-task -> QuantumResourceManager
    static class ApiProcessTaskHandler implements HttpHandler {
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
                LOG.info("API /process-task received task: " + task);
                int result = QRM.processTask(task);
                PM.logPerformance("QuantumResourceManager", System.currentTimeMillis() - start);
                String resp = "{\"result\":" + result + ",\"task\":\"" + escapeJson(task) + "\"}";
                LOG.info("Task processed: " + task + " -> " + result);
                sendJson(exchange, 200, resp);
            } catch (Exception e) {
                LOG.error("ApiProcessTaskHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // API: monitor-security -> AdaptiveSecurityEngine
    static class ApiMonitorSecurityHandler implements HttpHandler {
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
                LOG.info("API /monitor-security event: " + event);
                ASE.monitorEvent(event);
                sendJson(exchange, 200, "{\"status\":\"ok\",\"event\":\"" + escapeJson(event) + "\"}");
            } catch (Exception e) {
                LOG.error("ApiMonitorSecurityHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // API: register-user -> DecentralizedIdentityManager (returns MFA)
    static class ApiRegisterUserHandler implements HttpHandler {
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
                LOG.info("API /register-user userId: " + userId);
                DIM.registerUser(userId, publicKey);
                int token = DIM.generateMfaToken(userId);
                sendJson(exchange, 200, "{\"userId\":\"" + escapeJson(userId) + "\",\"mfa\":" + token + "}");
            } catch (Exception e) {
                LOG.error("ApiRegisterUserHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // API: verify-user -> checks MFA
    static class ApiVerifyUserHandler implements HttpHandler {
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
                LOG.info("API /verify-user userId: " + userId);
                boolean verified = DIM.verifyUser(userId, publicKey, mfa);
                sendJson(exchange, 200, "{\"verified\":" + verified + "}");
            } catch (Exception e) {
                LOG.error("ApiVerifyUserHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // API: secure-exchange -> encrypt message and simulate transmit
    static class ApiSecureExchangeHandler implements HttpHandler {
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
                LOG.info("API /secure-exchange -> recipient: " + recipient);
                String encrypted = AAF.secureDataExchange(message, recipient);
                PM.logPerformance("AutonomousApiFramework", System.currentTimeMillis() - start);
                sendJson(exchange, 200, "{\"recipient\":\"" + escapeJson(recipient) + "\",\"payload\":\"" + escapeJson(encrypted) + "\"}");
            } catch (Exception e) {
                LOG.error("ApiSecureExchangeHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // API: performance logs
    static class ApiPerformanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String json = PM.toJson();
                sendJson(exchange, 200, json);
            } catch (Exception e) {
                LOG.error("ApiPerformanceHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // API: logs recent
    static class ApiLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            try {
                String json = LOG.getRecentLogsJson();
                sendJson(exchange, 200, json);
            } catch (Exception e) {
                LOG.error("ApiLogsHandler error: " + e.getMessage(), e);
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /* ----------------------
       Utilities & Subsystems
       ---------------------- */

    // Safe read of request body
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

    /* ----------------------
       QuantumResourceManager
       (Simulated hybrid processing)
       ---------------------- */
    static class QuantumResourceManager {
        private final Random rnd;
        private final Map<String, Integer> taskLog; // recent results
        private final LogService logger;

        public QuantumResourceManager(LogService logger) {
            this.logger = logger;
            this.rnd = new SecureRandom();
            this.taskLog = new ConcurrentHashMap<>();
        }

        // returns numeric "result" for a task
        public int processTask(String task) throws InterruptedException {
            Objects.requireNonNull(task, "task required");
            long started = System.currentTimeMillis();
            logger.info("[QRM] Processing task: " + task);
            boolean quantum = rnd.nextBoolean();
            int result;
            if (quantum) {
                result = simulateQuantum(task);
            } else {
                result = simulateClassical(task);
            }
            taskLog.put(task, result);
            long elapsed = System.currentTimeMillis() - started;
            logger.info("[QRM] Task '" + task + "' completed in " + elapsed + "ms (quantum=" + quantum + ")");
            return result;
        }

        private int simulateQuantum(String task) throws InterruptedException {
            // simulate noisy, probabilistic behavior
            Thread.sleep(60 + rnd.nextInt(100));
            int r = task.hashCode() ^ rnd.nextInt();
            return Math.abs(r);
        }

        private int simulateClassical(String task) {
            // deterministic-ish with jitter
            int r = Math.abs(task.hashCode() + rnd.nextInt(1000));
            try { Thread.sleep(20 + rnd.nextInt(40)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return r;
        }
    }

    /* ----------------------
       ThreatDatabase (simple)
       ---------------------- */
    static class ThreatDatabase {
        private final Set<String> known = ConcurrentHashMap.newKeySet();
        private final LogService logger;

        public ThreatDatabase(LogService logger) {
            this.logger = logger;
            // seed with some patterns
            known.add("threat");
            known.add("exploit");
            known.add("malware");
            known.add("unauthorized");
        }

        public boolean isThreat(String event) {
            if (event == null) return false;
            String lower = event.toLowerCase(Locale.ROOT);
            for (String p : known) {
                if (lower.contains(p)) {
                    logger.info("[ThreatDB] Pattern matched: " + p + " in event: " + event);
                    return true;
                }
            }
            return false;
        }

        public void addPattern(String p) {
            if (p != null && !p.trim().isEmpty()) {
                known.add(p.trim().toLowerCase(Locale.ROOT));
                logger.info("[ThreatDB] Added pattern: " + p);
            }
        }

        public Set<String> listPatterns() {
            return Collections.unmodifiableSet(known);
        }
    }

    /* ----------------------
       AdaptiveSecurityEngine
       ---------------------- */
    static class AdaptiveSecurityEngine {
        private final Queue<String> securityLog = new ConcurrentLinkedQueue<>();
        private final ThreatDatabase db;
        private final LogService logger;
        private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        public AdaptiveSecurityEngine(ThreatDatabase db, LogService logger) {
            this.db = Objects.requireNonNull(db);
            this.logger = logger;
        }

        public void monitorEvent(String event) {
            Objects.requireNonNull(event);
            securityLog.add(Instant.now().toString() + " - " + event);
            logger.info("[ASE] Monitoring event: " + event);
            pool.submit(() -> {
                try {
                    if (db.isThreat(event)) {
                        respondToThreat(event);
                    } else {
                        logger.info("[ASE] Event deemed secure: " + event);
                    }
                } catch (Exception e) {
                    logger.error("[ASE] Error monitoring event: " + e.getMessage(), e);
                }
            });
        }

        private void respondToThreat(String threat) {
            logger.info("[ASE] Threat detected: " + threat + ". Initiating response...");
            try {
                // simulated containment workflow
                Thread.sleep(120);
                logger.info("[ASE] Isolating affected resources for threat: " + threat);
                Thread.sleep(80);
                logger.info("[ASE] Quarantine and remediation steps executed for: " + threat);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.error("[ASE] Interrupted during threat response", ie);
            } catch (Exception e) {
                logger.error("[ASE] Exception in respondToThreat: " + e.getMessage(), e);
            }
        }

        public List<String> getSecurityLog() {
            return new ArrayList<>(securityLog);
        }
    }

    /* ----------------------
       DecentralizedIdentityManager
       ---------------------- */
    static class DecentralizedIdentityManager {
        private final Map<String, String> ledger = new ConcurrentHashMap<>(); // userId -> publicKey
        private final Map<String, Integer> mfaStore = new ConcurrentHashMap<>();
        private final SecureRandom rnd = new SecureRandom();
        private final LogService logger;

        public DecentralizedIdentityManager(LogService logger) {
            this.logger = logger;
        }

        public void registerUser(String userId, String publicKey) {
            if (userId == null || publicKey == null) throw new IllegalArgumentException("userId and publicKey required");
            ledger.put(userId, publicKey);
            logger.info("[DIM] Registered user: " + userId);
        }

        public int generateMfaToken(String userId) {
            if (!ledger.containsKey(userId)) {
                logger.warn("[DIM] generateMfaToken for unknown user: " + userId);
                throw new IllegalArgumentException("user not registered");
            }
            int token = 100_000 + rnd.nextInt(900_000);
            mfaStore.put(userId, token);
            logger.info("[DIM] MFA token generated for " + userId + ": " + token);
            return token;
        }

        public boolean verifyUser(String userId, String publicKey, int mfaToken) {
            String pk = ledger.get(userId);
            boolean ok = pk != null && pk.equals(publicKey);
            if (!ok) {
                logger.warn("[DIM] publicKey mismatch for " + userId);
                return false;
            }
            Integer expected = mfaStore.get(userId);
            boolean verified = expected != null && expected == mfaToken;
            logger.info("[DIM] verifyUser(" + userId + "): " + verified);
            return verified;
        }

        public Set<String> listUsers() {
            return new HashSet<>(ledger.keySet());
        }
    }

    /* ----------------------
       AutonomousApiFramework (AES-GCM encryption)
       ---------------------- */
    static class AutonomousApiFramework {
        private final LogService logger;
        private final SecretKey masterKey; // AES key
        private final SecureRandom rnd = new SecureRandom();

        // AES-GCM parameters
        private static final int GCM_TAG_LENGTH = 128; // bits
        private static final int IV_LENGTH_BYTES = 12;

        public AutonomousApiFramework(LogService logger) {
            this.logger = logger;
            this.masterKey = generateAesKey();
            logger.info("[AAF] Autonomous API Framework initialized with AES key.");
        }

        private SecretKey generateAesKey() {
            try {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                try {
                    kg.init(256); // try 256-bit first
                } catch (InvalidParameterException ipe) {
                    kg.init(128); // fallback
                }
                return kg.generateKey();
            } catch (NoSuchAlgorithmException e) {
                // fallback to simple deterministic safe key (shouldn't happen on modern JVMs)
                logger.error("[AAF] AES algorithm not available. Using fallback key.", e);
                byte[] fallback = new byte[16];
                rnd.nextBytes(fallback);
                return new SecretKeySpec(fallback, "AES");
            }
        }

        // returns base64-encoded AES-GCM ciphertext (IV + ciphertext)
        public String secureDataExchange(String message, String recipientId) throws GeneralSecurityException {
            Objects.requireNonNull(message);
            Objects.requireNonNull(recipientId);
            try {
                logger.info("[AAF] Encrypting message for " + recipientId);
                byte[] iv = new byte[IV_LENGTH_BYTES];
                rnd.nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);
                byte[] ct = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(iv);
                baos.write(ct);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                logger.info("[AAF] Message encrypted for " + recipientId + ". (length=" + b64.length() + ")");
                // simulate transmit
                logger.info("[AAF] Transmitting encrypted payload to " + recipientId);
                return b64;
            } catch (GeneralSecurityException e) {
                logger.error("[AAF] Encryption error: " + e.getMessage(), e);
                throw e;
            } catch (IOException ioe) {
                logger.error("[AAF] IO error during encryption: " + ioe.getMessage(), ioe);
                throw new GeneralSecurityException(ioe);
            }
        }
    }

    /* ----------------------
       PerformanceMonitor
       ---------------------- */
    static class PerformanceMonitor {
        private final Map<String, Long> performanceLog = new ConcurrentHashMap<>();
        private final LogService logger;
        public PerformanceMonitor(LogService logger) {
            this.logger = logger;
        }
        public void logPerformance(String component, long timeTakenMs) {
            performanceLog.put(component, timeTakenMs);
            logger.info("[PM] " + component + " execution time: " + timeTakenMs + "ms");
        }
        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Long> e : performanceLog.entrySet()) {
                parts.add("\"" + escapeJson(e.getKey()) + "\":" + e.getValue());
            }
            sb.append(String.join(",", parts));
            sb.append("}");
            return sb.toString();
        }
    }

    /* ----------------------
       LogService (central logger + SSE)
       ---------------------- */
    static class LogService {
        private final Deque<String> recent;
        private final int capacity;
        private final Map<String, PrintWriter> sseClients = new ConcurrentHashMap<>();
        private final AtomicInteger counter = new AtomicInteger(0);

        public LogService(int capacity) {
            this.capacity = Math.max(100, capacity);
            this.recent = new ConcurrentLinkedDeque<>();
        }

        public void info(String msg) {
            log("INFO", msg, null);
        }

        public void warn(String msg) {
            log("WARN", msg, null);
        }

        public void error(String msg, Throwable t) {
            log("ERROR", msg, t);
        }

        private synchronized void log(String level, String msg, Throwable t) {
            String ts = Instant.now().toString();
            String id = String.valueOf(counter.incrementAndGet());
            String line = String.format("%s [%s] %s - %s", ts, level, id, msg);
            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                line += "\n" + sw.toString();
            }
            if (recent.size() >= capacity) recent.removeFirst();
            recent.addLast(line);
            // broadcast to SSE clients
            broadcastSse("log", "{\"level\":\"" + level + "\",\"message\":\"" + escapeJson(msg) + "\",\"ts\":\"" + ts + "\"}");
            // also print to stdout
            if ("ERROR".equals(level)) {
                System.err.println(line);
            } else {
                System.out.println(line);
            }
        }

        public String getRecentLogsJson() {
            List<String> copy = new ArrayList<>(recent);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"logs\":[");
            sb.append(copy.stream().map(s -> {
                String ts = s.split(" ", 2)[0];
                String msg = s.replace("\"", "\\\"");
                return "\"" + escapeJson(s) + "\"";
            }).collect(Collectors.joining(",")));
            sb.append("]}");
            return sb.toString();
        }

        // SSE management
        public void registerSseClient(String clientId, PrintWriter writer) {
            sseClients.put(clientId, writer);
            sendEventToClient(clientId, "connected", "{\"clientId\":\"" + escapeJson(clientId) + "\"}");
        }

        public void unregisterSseClient(String clientId) {
            sseClients.remove(clientId);
        }

        // broadcast to all SSE clients
        private void broadcastSse(String event, String jsonPayload) {
            sseClients.forEach((id, pw) -> {
                try {
                    pw.println("event: " + event);
                    pw.println("data: " + jsonPayload);
                    pw.println();
                    pw.flush();
                } catch (Exception e) {
                    // if client broken, remove
                    sseClients.remove(id);
                }
            });
        }

        public void sendEventToClient(String clientId, String event, String jsonPayload) {
            PrintWriter pw = sseClients.get(clientId);
            if (pw != null) {
                try {
                    pw.println("event: " + event);
                    pw.println("data: " + jsonPayload);
                    pw.println();
                    pw.flush();
                } catch (Exception e) {
                    sseClients.remove(clientId);
                }
            }
        }
    }

    /* ----------------------
       FrontendResource (embedded SPA)
       Single page: HTML/CSS/JS bundled into string
       ---------------------- */
    static class FrontendResource {
        public static String getIndexHtml() {
            return INDEX_HTML;
        }

        private static final String INDEX_HTML = 
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\" />\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
            "  <title>DUKE QAOS-AI — Console</title>\n" +
            "  <style>\n" +
            "    :root{--bg:#0b0f14;--card:#0f1720;--muted:#9aa6b2;--accent:#7c5cff;--danger:#ff6b6b;--glass: rgba(255,255,255,0.04)}\n" +
            "    *{box-sizing:border-box}\n" +
            "    body{margin:0;background:linear-gradient(180deg,#041024 0%, #071625 100%);color:#e6eef6;font-family:Inter,Segoe UI,Helvetica,Arial,sans-serif}\n" +
            "    .app{padding:20px;max-width:1200px;margin:12px auto}\n" +
            "    .header{display:flex;justify-content:space-between;align-items:center;margin-bottom:18px}\n" +
            "    .brand{display:flex;gap:12px;align-items:center}\n" +
            "    .logo{width:48px;height:48px;border-radius:10px;background:linear-gradient(135deg,var(--accent),#00d4ff);display:flex;align-items:center;justify-content:center;color:#020617;font-weight:700}\n" +
            "    .title{font-size:18px;font-weight:700}\n" +
            "    .subtitle{font-size:12px;color:var(--muted)}\n" +
            "    .grid{display:grid;grid-template-columns:1fr 420px;gap:16px}\n" +
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
            "\n" +
            "  <div class=\"grid\">\n" +
            "    <div>\n" +
            "      <div class=\"card\">\n" +
            "        <div style=\"display:flex;justify-content:space-between;align-items:center;margin-bottom:12px\">\n" +
            "          <div><strong>System Control</strong><div class=\"muted small\">Trigger tasks / security scans / register users</div></div>\n" +
            "          <div class=\"kpi\">\n" +
            "            <div class=\"item\"><div class=\"muted small\">Version</div><div id=\"k-version\">loading</div></div>\n" +
            "            <div class=\"item\"><div class=\"muted small\">Uptime</div><div id=\"k-uptime\">0s</div></div>\n" +
            "          </div>\n" +
            "        </div>\n" +
            "\n" +
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
            "\n" +
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
            "          </div>\n            <div style=\"flex:1\">\n" +
            "              <label class=\"small muted\">Public Key</label>\n" +
            "              <input id=\"user-pub\" value=\"publicKeyExample\" />\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"controls\">\n" +
            "          <button id=\"btn-get-logs\">Refresh Logs</button>\n" +
            "          <button id=\"btn-get-performance\">Refresh Perf</button>\n" +
            "        </div>\n            <div style=\"margin-top:10px\">\n" +
            "              <div class=\"muted small\">Recent System Logs</div>\n" +
            "              <div class=\"logs\" id=\"log-window\">Loading logs…</div>\n" +
            "            </div>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "\n" +
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
            "        <div class=\"controls\"><button id=\"btn-secure-send\">Encrypt & Send</button></div>\n            <div style=\"margin-top:10px\">\n            <div class=\"muted small\">Encrypted Payload</div>\n            <div style=\"padding:8px;background:rgba(255,255,255,0.02);border-radius:8px;margin-top:6px;word-break:break-all\" id=\"encrypted-payload\">—</div>\n            </div>\n            <footer>© 2024 Devin B. Royal — DUKE QAOS-AI Demo</footer>\n      </div>\n    </div>\n" +
            "  </div>\n" +
            "</div>\n" +
            "<script>\n" +
            "(() => {\n" +
            "  const versionEl = document.getElementById('k-version');\n" +
            "  const uptimeEl = document.getElementById('k-uptime');\n            \n" +
            "  versionEl.textContent = '" + APP_VERSION.replace("\"", "\\\"") + "';\n" +
            "  const startTs = Date.now();\n" +
            "  setInterval(() => { let s = Math.floor((Date.now()-startTs)/1000); uptimeEl.textContent = s + 's'; }, 1000);\n" +
            "\n" +
            "  // Utility fetch wrapper\n" +
            "  async function postJson(url, obj){\n" +
            "    try{\n" +
            "      const r = await fetch(url, {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(obj)});\n" +
            "      return await r.json();\n" +
            "    }catch(e){console.error('postJson error',e);return {error:e.message}};\n" +
            "  }\n" +
            "\n" +
            "  // run task\n" +
            "  document.getElementById('btn-run-task').addEventListener('click', async () => {\n" +
            "    const task = document.getElementById('task-name').value || 'AI Task';\n" +
            "    const res = await postJson('/api/process-task',{task});\n" +
            "    if(res.error){document.getElementById('task-result').textContent = 'Error: '+res.error;}\n" +
            "    else{document.getElementById('task-result').textContent = 'Result: '+res.result+' — '+res.task}\n" +
            "    refreshPerformance(); refreshLogs();\n" +
            "  });\n" +
            "\n" +
            "  // security scan\n" +
            "  document.getElementById('btn-scan-security').addEventListener('click', async () => {\n" +
            "    const ev = document.getElementById('security-event').value || 'Routine Check';\n" +
            "    const sev = document.getElementById('security-severity').value || 'info';\n" +
            "    const event = ev + ' ['+sev+']';\n" +
            "    const res = await postJson('/api/monitor-security',{event});\n" +
            "    if(res.error) alert('Error: '+res.error); else { refreshLogs(); }\n" +
            "  });\n" +
            "\n" +
            "  // register user\n" +
            "  document.getElementById('btn-register-user').addEventListener('click', async () => {\n" +
            "    const userId = document.getElementById('user-id').value || ('u'+Math.floor(Math.random()*10000));\n" +
            "    const pub = document.getElementById('user-pub').value || ('pub-'+userId);\n" +
            "    const res = await postJson('/api/register-user',{userId, publicKey:pub});\n            \n" +
            "    if(res.error) alert('Error: '+res.error); else {\n" +
            "      alert('Registered '+res.userId+' — MFA: '+res.mfa);\n" +
            "      refreshLogs();\n            \n" +
            "    }\n" +
            "  });\n" +
            "\n" +
            "  document.getElementById('btn-get-logs').addEventListener('click', refreshLogs);\n" +
            "  document.getElementById('btn-get-performance').addEventListener('click', refreshPerformance);\n" +
            "\n" +
            "  // Secure exchange\n" +
            "  document.getElementById('btn-secure-send').addEventListener('click', async () => {\n" +
            "    const recipient = document.getElementById('secure-recipient').value || 'Recipient';\n" +
            "    const message = document.getElementById('secure-msg').value || '';\n" +
            "    const res = await postJson('/api/secure-exchange',{recipient, message});\n" +
            "    if(res.error) document.getElementById('encrypted-payload').textContent = 'Error: '+res.error;\n" +
            "    else document.getElementById('encrypted-payload').textContent = res.payload || '—';\n" +
            "    refreshLogs();\n" +
            "  });\n" +
            "\n" +
            "  // SSE connection\n" +
            "  const eventsBox = document.getElementById('events-box');\n" +
            "  try{\n" +
            "    const sse = new EventSource('/events');\n" +
            "    sse.addEventListener('log', ev => { const d = JSON.parse(ev.data); appendEvent('LOG: '+d.level+' — '+d.message); });\n" +
            "    sse.addEventListener('connected', ev => { const d = JSON.parse(ev.data); appendEvent('Connected: '+d.clientId); });\n" +
            "    sse.addEventListener('heartbeat', ev => { /* no-op */ });\n" +
            "    sse.onerror = (e) => { appendEvent('SSE connection error'); sse.close(); setTimeout(()=>connectSseAgain(),3000); };\n" +
            "    function connectSseAgain(){ try{ new EventSource('/events'); }catch(e){} }\n" +
            "  }catch(e){ appendEvent('SSE not supported: '+e.message); }\n" +
            "\n" +
            "  function appendEvent(text){ const el = document.createElement('div'); el.textContent = (new Date()).toLocaleTimeString() + ' — ' + text; eventsBox.prepend(el); }\n" +
            "\n" +
            "  // logs & perf\n" +
            "  async function refreshLogs(){ try{ const r = await fetch('/api/logs'); const j = await r.json(); const lw = document.getElementById('log-window'); lw.innerHTML = ''; j.logs.slice().reverse().forEach(l => { const el = document.createElement('div'); el.textContent = l; lw.appendChild(el); }); }catch(e){console.error(e);} }\n" +
            "  async function refreshPerformance(){ try{ const r = await fetch('/api/performance'); const j = await r.json(); drawPerfChart(j); }catch(e){console.error(e);} }\n" +
            "\n" +
            "  // Simple perf chart rendering\n" +
            "  function drawPerfChart(data){ const canvas = document.getElementById('perf-chart'); if(!canvas) return; const ctx = canvas.getContext('2d'); const w = canvas.width; const h = canvas.height; ctx.clearRect(0,0,w,h); ctx.fillStyle='rgba(255,255,255,0.02)'; ctx.fillRect(0,0,w,h); const keys = Object.keys(data); if(keys.length===0){ ctx.fillStyle='#9aa6b2'; ctx.font='12px monospace'; ctx.fillText('No perf data',10,20); return; } const vals = keys.map(k=>data[k]); const max = Math.max(...vals,1); const barW = Math.floor(w / keys.length) - 6; keys.forEach((k,i)=>{ const v = data[k]; const bh = Math.max(4, Math.floor((v/max)*(h-20))); const x = i*(barW+6)+6; const y = h - bh - 6; ctx.fillStyle='rgba(124,92,255,0.95)'; ctx.fillRect(x,y,barW,bh); ctx.fillStyle='#9aa6b2'; ctx.font='10px monospace'; ctx.fillText(k, x, h-2); }); }\n" +
            "\n" +
            "  // initial load\n" +
            "  refreshLogs(); refreshPerformance();\n" +
            "})();\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>\n";
    }

    /* ----------------------
       Minimal JSON utility (no external deps)
       - parse simple flat JSON objects with string/number values
       ---------------------- */
    static class JsonUtil {
        // Very small parser — expects {"k":"v","k2":123} with no nested objects or arrays
        public static Map<String, String> parseSimpleJsonToMap(String json) {
            Map<String, String> map = new HashMap<>();
            if (json == null) return map;
            String s = json.trim();
            if (s.startsWith("{")) s = s.substring(1);
            if (s.endsWith("}")) s = s.substring(0, s.length()-1);
            // naive split by commas not in quotes
            List<String> parts = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '\"') inQuotes = !inQuotes;
                if (c == ',' && !inQuotes) {
                    parts.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
            if (cur.length() > 0) parts.add(cur.toString());
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
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) return s.substring(1, s.length()-1);
            // numbers or barewords
            return s;
        }
    }

} // end of QAOSAI_SingleFile class

/* Copyright © 2024 Devin B. Royal. All Rights Reserved. */
