package com.yourcompany.uac.mitigation;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Lightweight alert router that can broadcast to staff or console depending on
 * config. In the stub environment this is log-only, but the structure mirrors
 * what we will wire to Adventure audiences later.
 */
public class AlertManager {

    private final UltimateAntiCheatPlugin plugin;
    private final Path logDir;

    public AlertManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.logDir = plugin.getDataFolder().toPath().resolve("logs");
        try {
            Files.createDirectories(logDir);
        } catch (IOException ignored) {
        }
    }

    public void alert(String message, int severity) {
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.alertsEnabled || severity < settings.alertMinSeverity) {
            return;
        }

        plugin.getLogger().log(Level.INFO, message);
        append("flags.log", message);
        // TODO: Replace with Adventure broadcast when Paper is on the classpath.
        for (Player online : Bukkit.getServer().getPluginManager().getOnlinePlayers()) {
            if (online.hasPermission(settings.notifyPermission)) {
                online.sendMessage(message);
            }
        }
    }

    public void log(String message, Level level) {
        plugin.getLogger().log(level, message);
        String target = level.intValue() >= Level.WARNING.intValue() ? "mitigations.log" : "general.log";
        append(target, message);
    }

    public void logTrustChange(String message) {
        append("trust-changes.log", message);
    }

    private void append(String fileName, String line) {
        try {
            Files.writeString(logDir.resolve(fileName), line + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
