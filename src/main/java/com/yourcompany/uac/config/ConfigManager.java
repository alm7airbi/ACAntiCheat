package com.yourcompany.uac.config;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Handles loading and refreshing configuration files.
 */
public class ConfigManager {

    private final UltimateAntiCheatPlugin plugin;
    private Settings settings;

    public ConfigManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration configuration = plugin.getConfig();
        this.settings = Settings.fromYaml(configuration);
    }

    public Settings getSettings() {
        return settings;
    }
}
