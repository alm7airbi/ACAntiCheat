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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Alert router for staff and external sinks. In stub mode this degrades to
 * console logging only; in Paper mode staff messages respect permissions and
 * optionally dispatch to Discord webhooks.
 */
public class AlertManager {

    private final UltimateAntiCheatPlugin plugin;
    private final Path logDir;
    private final Map<String, Long> throttleWindows = new ConcurrentHashMap<>();
    private volatile String lastWebhookStatus = "disabled";

    public AlertManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.logDir = plugin.getDataFolder().toPath().resolve("logs");
        try {
            Files.createDirectories(logDir);
        } catch (IOException ignored) {
        }
    }

    public void alert(String playerName, String checkName, String message, int severity, PlayerCheckState.MitigationLevel level) {
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.alertsEnabled || severity < settings.alertMinSeverity) {
            return;
        }
        if (shouldThrottle(playerName, checkName, settings.alertThrottleSeconds)) {
            return;
        }
        String formatted = prefix(settings, message, severity, level);
        if (settings.alertConsole) {
            plugin.getLogger().log(Level.INFO, formatted);
        }
        append("flags.log", formatted);

        if (settings.alertStaff) {
            for (Player online : Bukkit.getServer().getPluginManager().getOnlinePlayers()) {
                if (online.hasPermission(settings.alertPermission)) {
                    online.sendMessage(formatted);
                }
            }
        }

        if (settings.alertDiscord && settings.discordWebhookUrl != null && !settings.discordWebhookUrl.isBlank()) {
            sendWebhook(settings, playerName, checkName, message, severity, level);
        }
    }

    public void log(String message, Level level) {
        if (plugin.getConfigManager().getSettings().alertConsole) {
            plugin.getLogger().log(level, message);
        }
        String target = level.intValue() >= Level.WARNING.intValue() ? "mitigations.log" : "general.log";
        append(target, message);
    }

    public void logTrustChange(String message) {
        append("trust-changes.log", message);
    }

    public String getLastWebhookStatus() {
        return lastWebhookStatus;
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

    private String prefix(Settings settings, String message, int severity, PlayerCheckState.MitigationLevel level) {
        String sev = "§e" + severity;
        String mit = level != null && level != PlayerCheckState.MitigationLevel.NONE ? " §7| Mit=" + level : "";
        return "[" + settings.alertChannelName + "] §f" + message + " §7(sev=" + sev + mit + ")";
    }

    private void sendWebhook(Settings settings, String player, String check, String message, int severity, PlayerCheckState.MitigationLevel level) {
        if (shouldThrottle(player + "#webhook", check, settings.alertDiscordThrottleSeconds)) {
            return;
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            String payload = "{\"player\":\"" + player + "\"," +
                    "\"check\":\"" + check + "\"," +
                    "\"severity\":" + severity + "," +
                    "\"mitigation\":\"" + (level == null ? "NONE" : level) + "\"," +
                    "\"message\":\"" + message.replace("\"", "'") + "\"," +
                    "\"timestamp\":\"" + Instant.now() + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.discordWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(resp -> lastWebhookStatus = "HTTP " + resp.statusCode())
                    .exceptionally(ex -> {
                        lastWebhookStatus = "error: " + ex.getMessage();
                        return null;
                    });
        } catch (IllegalArgumentException ex) {
            lastWebhookStatus = "invalid webhook URL";
        }
    }

    private void append(String fileName, String line) {
        try {
            Path target = logDir.resolve(fileName);
            rotateIfNeeded(target);
            Files.writeString(target, line + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private void rotateIfNeeded(Path file) throws IOException {
        Settings settings = plugin.getConfigManager().getSettings();
        long maxBytes = Math.max(settings.logMaxBytes, 0);
        if (maxBytes <= 0) {
            return;
        }
        if (Files.exists(file) && Files.size(file) > maxBytes) {
            int maxFiles = Math.max(settings.logMaxFiles, 1);
            for (int i = maxFiles - 1; i >= 0; i--) {
                Path rotated = file.resolveSibling(file.getFileName().toString() + "." + i);
                if (Files.exists(rotated)) {
                    if (i + 1 >= maxFiles) {
                        Files.deleteIfExists(rotated);
                    } else {
                        Files.move(rotated, file.resolveSibling(file.getFileName().toString() + "." + (i + 1)), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            Files.move(file, file.resolveSibling(file.getFileName().toString() + ".0"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
