package com.github.luischavez.videodownloader.app.remote;

import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.support.FFMPEGTask;
import com.github.luischavez.videodownloader.task.Task;
import com.github.luischavez.videodownloader.task.TaskManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RemoteApiServer {

    private static final String CONFIGURATION_FILE = "remote-api.properties";
    private static final String LOG_FILE = "logs/m3u8.log";
    private static final int DEFAULT_PORT = 8787;
    private static final int MAX_LOG_LINES = 500;

    private final AppContext context;
    private final Gson gson;
    private final long startedAt;
    private final String bindAddress;
    private final int port;
    private final String token;
    private final ExecutorService executor;

    private HttpServer server;

    private RemoteApiServer(AppContext context, Properties properties) {
        this.context = context;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.startedAt = System.currentTimeMillis();
        this.bindAddress = properties.getProperty("bind", "127.0.0.1").trim();
        this.port = parsePort(properties.getProperty("port"));
        this.token = properties.getProperty("token", "").trim();
        this.executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "RemoteApiServer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static RemoteApiServer startIfConfigured(AppContext context) {
        File configurationFile = new File(context.buildPath(context.getWorkingDir(), CONFIGURATION_FILE));
        if (!configurationFile.isFile()) {
            context.debug(RemoteApiServer.class, "remote API disabled; remote-api.properties not found");
            return null;
        }

        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(configurationFile)) {
            properties.load(input);

            if (!Boolean.parseBoolean(properties.getProperty("enabled", "false"))) {
                context.debug(RemoteApiServer.class, "remote API disabled by configuration");
                return null;
            }

            RemoteApiServer remoteApiServer = new RemoteApiServer(context, properties);
            remoteApiServer.start();
            return remoteApiServer;
        } catch (Exception ex) {
            context.error(RemoteApiServer.class, "can't start remote API: " + ex.getMessage(), ex);
            return null;
        }
    }

    private static int parsePort(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_PORT;
        }

        int parsed = Integer.parseInt(value.trim());
        if (parsed < 1 || parsed > 65535) {
            throw new IllegalArgumentException("remote API port must be between 1 and 65535");
        }
        return parsed;
    }

    private void start() throws IOException {
        if (token.length() < 16) {
            throw new IllegalArgumentException("remote API token must contain at least 16 characters");
        }

        server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new DashboardHandler());
        server.setExecutor(executor);
        server.start();

        context.debug(RemoteApiServer.class,
                String.format("remote API listening on http://%s:%d", bindAddress, port));
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
        executor.shutdownNow();
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String suppliedToken = exchange.getRequestHeaders().getFirst("X-API-Token");
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");

        if ((suppliedToken == null || suppliedToken.isEmpty())
                && authorization != null && authorization.startsWith("Bearer ")) {
            suppliedToken = authorization.substring("Bearer ".length()).trim();
        }

        return suppliedToken != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8",
                gson.toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        sendJson(exchange, status, body);
    }

    private void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private StreamConfiguration findStream(long uid) {
        Object configuration = context.getSystem().getManager(ConfigurationManager.class).find(uid);
        return configuration instanceof StreamConfiguration ? (StreamConfiguration) configuration : null;
    }

    private Map<String, Object> streamResponse(StreamConfiguration configuration) {
        Task task = context.getSystem().getManager(TaskManager.class).get(configuration.uid());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("uid", configuration.uid());
        response.put("alias", configuration.getAlias());
        response.put("country", configuration.getCountry());
        response.put("url", configuration.getUrl());
        response.put("enabled", configuration.isEnabled());
        response.put("type", configuration.getType());
        response.put("quality", configuration.getPreferredQuality());
        response.put("scheduleWhenAvailable", configuration.isScheduleWhenAvailable());
        response.put("status", taskStatus(configuration, task));
        response.put("pid", task != null && task.isRunning() ? task.pid() : null);
        response.put("details", task != null ? task.details() : null);

        if (task instanceof FFMPEGTask) {
            FFMPEGTask ffmpegTask = (FFMPEGTask) task;
            response.put("outputFile", ffmpegTask.getFileName());
        } else {
            response.put("outputFile", null);
        }
        return response;
    }

    private String taskStatus(StreamConfiguration configuration, Task task) {
        if (!configuration.isEnabled()) {
            return "disabled";
        }
        if (task != null && task.isRunning()) {
            return "recording";
        }
        return "waiting";
    }

    private List<Map<String, Object>> streamList() {
        List<StreamConfiguration> configurations = context.getSystem()
                .getManager(ConfigurationManager.class)
                .list(StreamConfiguration.class);
        configurations.sort(Comparator.comparing(
                StreamConfiguration::getAlias,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<Map<String, Object>> response = new ArrayList<>();
        for (StreamConfiguration configuration : configurations) {
            response.add(streamResponse(configuration));
        }
        return response;
    }

    private List<Map<String, Object>> taskList() {
        List<Map<String, Object>> response = new ArrayList<>();
        for (StreamConfiguration configuration : context.getSystem()
                .getManager(ConfigurationManager.class)
                .list(StreamConfiguration.class)) {
            Task task = context.getSystem().getManager(TaskManager.class).get(configuration.uid());
            if (task == null) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("uid", configuration.uid());
            item.put("alias", configuration.getAlias());
            item.put("running", task.isRunning());
            item.put("pid", task.isRunning() ? task.pid() : null);
            item.put("details", task.details());
            item.put("taskClass", task.getClass().getName());
            if (task instanceof FFMPEGTask) {
                FFMPEGTask ffmpegTask = (FFMPEGTask) task;
                item.put("outputFile", ffmpegTask.getFileName());
            }
            response.add(item);
        }
        response.sort(Comparator.comparing(item -> String.valueOf(item.get("alias")),
                String.CASE_INSENSITIVE_ORDER));
        return response;
    }

    private Map<String, Object> healthResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("timestamp", Instant.now().toString());
        response.put("uptimeSeconds", (System.currentTimeMillis() - startedAt) / 1000L);
        response.put("streams", streamList().size());
        response.put("tasks", taskList().size());
        return response;
    }

    private Map<String, Object> systemResponse() {
        File workingDirectory = new File(context.getWorkingDir());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        response.put("workingDirectory", workingDirectory.getAbsolutePath());
        response.put("freeDiskBytes", workingDirectory.getUsableSpace());
        response.put("totalDiskBytes", workingDirectory.getTotalSpace());
        response.put("processors", Runtime.getRuntime().availableProcessors());
        response.put("memoryFreeBytes", Runtime.getRuntime().freeMemory());
        response.put("memoryTotalBytes", Runtime.getRuntime().totalMemory());
        response.put("ffmpegAvailable", new File(context.buildPath(
                context.getWorkingDir(), "tools", "ffmpeg.exe")).isFile());
        response.put("ytDlpAvailable", new File(context.buildPath(
                context.getWorkingDir(), "youtube", "yt-dlp.exe")).isFile());
        response.put("bind", bindAddress);
        response.put("port", port);
        return response;
    }

    private List<String> readLogLines(int requestedLines) throws IOException {
        int limit = Math.max(1, Math.min(MAX_LOG_LINES, requestedLines));
        File logFile = new File(context.buildPath(context.getWorkingDir(), LOG_FILE));
        if (!logFile.isFile()) {
            return Collections.emptyList();
        }

        List<String> reversed = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(logFile, "r")) {
            long position = file.length() - 1;
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            while (position >= 0 && reversed.size() < limit) {
                file.seek(position--);
                int value = file.read();
                if (value == '\n') {
                    addLogLine(reversed, line);
                } else if (value != '\r') {
                    line.write(value);
                }
            }
            addLogLine(reversed, line);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private void addLogLine(List<String> target, ByteArrayOutputStream reversedLine) {
        if (reversedLine.size() == 0) {
            return;
        }
        byte[] reversedBytes = reversedLine.toByteArray();
        for (int left = 0, right = reversedBytes.length - 1; left < right; left++, right--) {
            byte current = reversedBytes[left];
            reversedBytes[left] = reversedBytes[right];
            reversedBytes[right] = current;
        }
        target.add(new String(reversedBytes, StandardCharsets.UTF_8));
        reversedLine.reset();
    }

    private int requestedLogLines(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 100;
        }
        for (String parameter : query.split("&")) {
            String[] parts = parameter.split("=", 2);
            if (parts.length == 2 && "limit".equals(parts[0])) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    return 100;
                }
            }
        }
        return 100;
    }

    private Map<String, Object> updateStream(StreamConfiguration configuration, String action) {
        ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);
        TaskManager taskManager = context.getSystem().getManager(TaskManager.class);

        if ("enable".equals(action)) {
            configuration.setEnabled(true);
            configurationManager.add(configuration, true);
        } else if ("disable".equals(action)) {
            configuration.setEnabled(false);
            configurationManager.add(configuration, true);
        } else if ("refresh".equals(action)) {
            context.updateSchedules(configuration);
        } else if ("restart".equals(action)) {
            if (!configuration.isEnabled()) {
                throw new IllegalStateException("stream is disabled");
            }
            if (taskManager.get(configuration.uid()) != null) {
                taskManager.remove(configuration.uid());
            }
            context.updateSchedules(configuration);
        } else {
            throw new IllegalArgumentException("unsupported action");
        }

        context.debug(RemoteApiServer.class,
                String.format("remote API action %s stream %s (%d)",
                        action, configuration.getAlias(), configuration.uid()));
        return streamResponse(configuration);
    }

    private final class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                if (!isAuthorized(exchange)) {
                    sendError(exchange, 401, "unauthorized");
                    return;
                }

                String method = exchange.getRequestMethod().toUpperCase();
                String path = exchange.getRequestURI().getPath();
                if ("GET".equals(method) && "/api/health".equals(path)) {
                    sendJson(exchange, 200, healthResponse());
                    return;
                }
                if ("GET".equals(method) && "/api/streams".equals(path)) {
                    sendJson(exchange, 200, streamList());
                    return;
                }
                if ("GET".equals(method) && "/api/tasks".equals(path)) {
                    sendJson(exchange, 200, taskList());
                    return;
                }
                if ("GET".equals(method) && "/api/logs".equals(path)) {
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("lines", readLogLines(requestedLogLines(exchange.getRequestURI().getQuery())));
                    sendJson(exchange, 200, response);
                    return;
                }
                if ("GET".equals(method) && "/api/system".equals(path)) {
                    sendJson(exchange, 200, systemResponse());
                    return;
                }

                String[] parts = path.split("/");
                if (parts.length >= 4 && "api".equals(parts[1]) && "streams".equals(parts[2])) {
                    long uid;
                    try {
                        uid = Long.parseLong(parts[3]);
                    } catch (NumberFormatException ex) {
                        sendError(exchange, 400, "invalid stream uid");
                        return;
                    }
                    StreamConfiguration configuration = findStream(uid);
                    if (configuration == null) {
                        sendError(exchange, 404, "stream not found");
                        return;
                    }

                    if ("GET".equals(method) && parts.length == 4) {
                        sendJson(exchange, 200, streamResponse(configuration));
                        return;
                    }
                    if ("POST".equals(method) && parts.length == 5) {
                        sendJson(exchange, 200, updateStream(configuration, parts[4]));
                        return;
                    }
                }
                sendError(exchange, 404, "endpoint not found");
            } catch (IllegalStateException | IllegalArgumentException ex) {
                sendError(exchange, 409, ex.getMessage());
            } catch (Exception ex) {
                context.error(RemoteApiServer.class, "remote API request failed: " + ex.getMessage(), ex);
                sendError(exchange, 500, "internal server error");
            } finally {
                exchange.close();
            }
        }
    }

    private final class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                    || !("/".equals(path) || "/index.html".equals(path))) {
                sendError(exchange, 404, "not found");
                return;
            }

            try (InputStream input = RemoteApiServer.class.getResourceAsStream("/remote/index.html")) {
                if (input == null) {
                    sendError(exchange, 500, "dashboard resource not found");
                    return;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                send(exchange, 200, "text/html; charset=utf-8", output.toByteArray());
            } finally {
                exchange.close();
            }
        }
    }
}
