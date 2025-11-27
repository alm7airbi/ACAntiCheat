package com.yourcompany.uac.mitigation;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Alert router for staff and external sinks. In stub mode this degrades to
 * console logging only; in Paper mode staff messages respect permissions and
 * optionally dispatch to Discord webhooks.
 */
public class AlertManager {

    private final UltimateAntiCheatPlugin plugin;
    private Path logDir;
    private volatile Settings settings;
    private final Map<String, Long> throttleWindows = new ConcurrentHashMap<>();
    private final List<AlertSink> sinks = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService asyncExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "acac-alerts");
        t.setDaemon(true);
        return t;
    });
    private volatile String lastWebhookStatus = "disabled";
    private volatile long lastWebhookAt;
    private volatile long lastWebhookErrorAt;
    private volatile String lastWebhookErrorMessage = "";
    private volatile long lastStructuredErrorAt;
    private volatile String lastStructuredErrorMessage = "";

    public AlertManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.logDir = resolveLogDir(settings);
        createLogDir();
        wireSinks(settings);
    }

    public void refresh(Settings newSettings) {
        this.settings = newSettings;
        this.logDir = resolveLogDir(newSettings);
        createLogDir();
        this.sinks.clear();
        wireSinks(newSettings);
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }

    public void alert(String playerName, String checkName, String message, int severity, PlayerCheckState.MitigationLevel level) {
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.alertsEnabled || severity < settings.alertMinSeverity) {
            return;
        }
        if (shouldThrottle(playerName, checkName, settings.alertThrottleSeconds)) {
            return;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("backend", plugin.getDatabaseManager().getPersistenceStatus());
        context.put("integration", plugin.getIntegrationService().name());
        context.put("webhookEnabled", settings.alertDiscord);
        AlertEvent event = new AlertEvent(Instant.now(), playerName, checkName, message, severity, level, context);

        sinks.forEach(sink -> sink.send(event));
    }

    public void log(String message, Level level) {
        if (settings.alertConsole) {
            plugin.getLogger().log(level, message);
        }
        AlertEvent event = new AlertEvent(Instant.now(), null, null, message, level.intValue(), null, Map.of("level", level.getName()));
        sinks.forEach(sink -> sink.send(event));
    }

    public void logTrustChange(String message) {
        AlertEvent event = new AlertEvent(Instant.now(), null, "trust", message, 1, null, Map.of("type", "trust-change"));
        sinks.forEach(sink -> sink.send(event));
    }

    public String getLastWebhookStatus() {
        return lastWebhookStatus;
    }

    public boolean isStructuredLoggingEnabled() {
        return settings.alertStructuredLogging;
    }

    public Path getStructuredLogPath() {
        return logDir.resolve(settings.alertStructuredFileName);
    }

    public long getStructuredMaxBytes() {
        return settings.alertStructuredMaxBytes;
    }

    public int getStructuredMaxFiles() {
        return settings.alertStructuredMaxFiles;
    }

    public int getStructuredRetentionDays() {
        return settings.alertStructuredRetentionDays;
    }

    private Path resolveLogDir(Settings settings) {
        Path configured = Path.of(settings.alertStructuredDirectory);
        return configured.isAbsolute() ? configured : plugin.getDataFolder().toPath().resolve(configured);
    }

    private void createLogDir() {
        try {
            Files.createDirectories(logDir);
        } catch (IOException ignored) {
        }
    }

    public long getLastWebhookAt() {
        return lastWebhookAt;
    }

    public long getLastWebhookErrorAt() {
        return lastWebhookErrorAt;
    }

    public String getLastWebhookErrorMessage() {
        return lastWebhookErrorMessage;
    }

    public long getLastStructuredErrorAt() {
        return lastStructuredErrorAt;
    }

    public String getLastStructuredErrorMessage() {
        return lastStructuredErrorMessage;
    }

    private boolean shouldThrottle(String player, String check, int windowSeconds) {
        if (windowSeconds <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        String key = player + ":" + check;
        Long last = throttleWindows.get(key);
        if (last != null && now - last < windowSeconds * 1000L) {
            return true;
        }
        throttleWindows.put(key, now);
        return false;
    }

    private void wireSinks(Settings settings) {
        sinks.clear();
        if (settings.alertConsole || settings.alertStaff) {
            sinks.add(new ConsoleStaffSink(settings));
        }
        if (settings.alertStructuredLogging) {
            sinks.add(new StructuredLogSink(settings));
        }
        if (settings.alertDiscord && settings.discordWebhookUrl != null && !settings.discordWebhookUrl.isBlank()) {
            sinks.add(new DiscordWebhookSink(settings));
            lastWebhookStatus = "configured";
        }
    }

    private String prefix(Settings settings, String message, int severity, PlayerCheckState.MitigationLevel level) {
        String sev = "§e" + severity;
        String mit = level != null && level != PlayerCheckState.MitigationLevel.NONE ? " §7| Mit=" + level : "";
        return "[" + settings.alertChannelName + "] §f" + message + " §7(sev=" + sev + mit + ")";
    }

    private class ConsoleStaffSink implements AlertSink {
        private final Settings settings;

        ConsoleStaffSink(Settings settings) {
            this.settings = settings;
        }

        @Override
        public void send(AlertEvent event) {
            String formatted = prefix(settings, event.getMessage(), event.getSeverity(), event.getMitigationLevel());
            if (settings.alertConsole) {
                plugin.getLogger().log(Level.INFO, formatted);
            }
            if (settings.alertStaff) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission(settings.alertPermission)) {
                        online.sendMessage(formatted);
                    }
                }
            }
            appendPlain(event, formatted);
        }
    }

    private class StructuredLogSink implements AlertSink {
        private final Settings settings;

        StructuredLogSink(Settings settings) {
            this.settings = settings;
        }

        @Override
        public void send(AlertEvent event) {
            asyncExecutor.execute(() -> {
                try {
                    Path target = logDir.resolve(settings.alertStructuredFileName);
                    rotateIfNeeded(target, settings.alertStructuredMaxBytes, settings.alertStructuredMaxFiles);
                    Map<String, Object> json = new HashMap<>();
                    json.put("ts", event.getTimestamp().toString());
                    json.put("player", event.getPlayerName());
                    json.put("check", event.getCheckName());
                    json.put("severity", event.getSeverity());
                    json.put("mitigation", event.getMitigationLevel() == null ? null : event.getMitigationLevel().name());
                    json.put("message", event.getMessage());
                    json.put("context", event.getContext());
                    String line = toJson(json);
                    Files.writeString(target, line + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND);
                    purgeOld(target, settings.alertStructuredRetentionDays);
                } catch (IOException ex) {
                    lastStructuredErrorAt = System.currentTimeMillis();
                    lastStructuredErrorMessage = ex.getMessage();
                }
            });
        }

        private void purgeOld(Path target, int retentionDays) throws IOException {
            if (retentionDays <= 0) {
                return;
            }
            long cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS).toEpochMilli();
            for (int i = 0; i < settings.alertStructuredMaxFiles; i++) {
                Path rotated = target.resolveSibling(target.getFileName().toString() + "." + i);
                if (Files.exists(rotated) && Files.getLastModifiedTime(rotated).toMillis() < cutoff) {
                    Files.deleteIfExists(rotated);
                }
            }
        }
    }

    private class DiscordWebhookSink implements AlertSink {
        private final Settings settings;
        private final HttpClient client = HttpClient.newHttpClient();

        DiscordWebhookSink(Settings settings) {
            this.settings = settings;
        }

        @Override
        public void send(AlertEvent event) {
            if (event.getSeverity() < settings.alertDiscordMinSeverity) {
                return;
            }
            if (shouldThrottle(event.getPlayerName() + "#webhook", event.getCheckName(), settings.alertDiscordThrottleSeconds)) {
                return;
            }
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(settings.discordWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(buildPayload(event), StandardCharsets.UTF_8))
                        .build();
                dispatchWithRetry(request, 0);
            } catch (IllegalArgumentException ex) {
                lastWebhookStatus = "invalid webhook URL";
                lastWebhookErrorAt = System.currentTimeMillis();
                lastWebhookErrorMessage = ex.getMessage();
            }
        }

        private void dispatchWithRetry(HttpRequest request, int attempt) {
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(resp -> {
                        lastWebhookStatus = "HTTP " + resp.statusCode();
                        lastWebhookAt = System.currentTimeMillis();
                    })
                    .exceptionally(ex -> {
                        lastWebhookStatus = "error";
                        lastWebhookErrorAt = System.currentTimeMillis();
                        lastWebhookErrorMessage = ex.getMessage();
                        int nextAttempt = attempt + 1;
                        if (nextAttempt <= settings.alertDiscordMaxRetries) {
                            long delay = settings.alertDiscordBackoffMillis * (long) Math.pow(2, attempt);
                            asyncExecutor.schedule(() -> dispatchWithRetry(request, nextAttempt), delay, TimeUnit.MILLISECONDS);
                        }
                        return null;
                    });
        }

        private String buildPayload(AlertEvent event) {
            if (!settings.alertDiscordDetailedPayload) {
                return "{\"content\":\"" + sanitize(event.getMessage()) + "\"}";
            }
            Map<String, Object> json = new HashMap<>();
            json.put("username", settings.alertChannelName);
            json.put("content", "");
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", event.getCheckName() == null ? "ACAC Alert" : event.getCheckName());
            embed.put("description", sanitize(event.getMessage()));
            embed.put("timestamp", event.getTimestamp().toString());
            List<Map<String, String>> fields = new ArrayList<>();
            fields.add(field("Player", event.getPlayerName()));
            fields.add(field("Severity", String.valueOf(event.getSeverity())));
            fields.add(field("Mitigation", event.getMitigationLevel() == null ? "NONE" : event.getMitigationLevel().name()));
            fields.add(field("Backend", String.valueOf(event.getContext().get("backend"))));
            fields.add(field("Integration", String.valueOf(event.getContext().get("integration"))));
            embed.put("fields", fields);
            json.put("embeds", List.of(embed));
            return toJson(json);
        }

        private Map<String, String> field(String name, String value) {
            Map<String, String> f = new HashMap<>();
            f.put("name", name);
            f.put("value", value == null ? "N/A" : value);
            f.put("inline", "true");
            return f;
        }

        private String sanitize(String raw) {
            return raw == null ? "" : raw.replace("\"", "'");
        }
    }

    private void rotateIfNeeded(Path file, long maxBytes, int maxFiles) throws IOException {
        long threshold = Math.max(maxBytes, 0);
        if (threshold <= 0) {
            return;
        }
        if (Files.exists(file) && Files.size(file) > threshold) {
            int max = Math.max(maxFiles, 1);
            for (int i = max - 1; i >= 0; i--) {
                Path rotated = file.resolveSibling(file.getFileName().toString() + "." + i);
                if (Files.exists(rotated)) {
                    if (i + 1 >= max) {
                        Files.deleteIfExists(rotated);
                    } else {
                        Files.move(rotated, file.resolveSibling(file.getFileName().toString() + "." + (i + 1)), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            Files.move(file, file.resolveSibling(file.getFileName().toString() + ".0"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void appendPlain(AlertEvent event, String line) {
        try {
            String fileName = event.getMitigationLevel() != null ? "mitigations.log" : (event.getCheckName() == null ? "general.log" : "flags.log");
            Path target = logDir.resolve(fileName);
            rotateIfNeeded(target, settings.alertStructuredMaxBytes, settings.alertStructuredMaxFiles);
            Files.writeString(target, line + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ex) {
            lastStructuredErrorAt = System.currentTimeMillis();
            lastStructuredErrorMessage = ex.getMessage();
        }
    }

    private String toJson(Map<String, ?> json) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : json.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(entry.getKey()).append('"').append(':');
            sb.append(encode(entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private String encode(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> coerced = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                coerced.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return toJson(coerced);
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(',');
                first = false;
                sb.append(encode(o));
            }
            sb.append(']');
            return sb.toString();
        }
        return '"' + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
