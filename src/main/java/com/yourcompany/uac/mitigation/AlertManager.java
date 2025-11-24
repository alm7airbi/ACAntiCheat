package com.yourcompany.uac.mitigation;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Lightweight alert router that can broadcast to staff or console depending on
 * config. In the stub environment this is log-only, but the structure mirrors
 * what we will wire to Adventure audiences later.
 */
public class AlertManager {

    private final UltimateAntiCheatPlugin plugin;

    public AlertManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void alert(String message, int severity) {
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.alertsEnabled || severity < settings.alertMinSeverity) {
            return;
        }

        plugin.getLogger().log(Level.INFO, message);
        // TODO: Replace with Adventure broadcast when Paper is on the classpath.
        for (Player online : Bukkit.getServer().getPluginManager().getOnlinePlayers()) {
            if (online.hasPermission(settings.notifyPermission)) {
                online.sendMessage(message);
            }
        }
    }

    public void log(String message, Level level) {
        plugin.getLogger().log(level, message);
    }
}
