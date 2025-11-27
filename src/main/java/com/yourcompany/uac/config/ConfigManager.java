package com.yourcompany.uac.config;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles loading and refreshing configuration files.
 */
public class ConfigManager {

    private final UltimateAntiCheatPlugin plugin;
    private Settings settings;
    private int loadedVersion = Settings.CURRENT_CONFIG_VERSION;
    private boolean migrationRan;
    private int migratedFrom = -1;
    private boolean regeneratedDefaults;
    private List<String> validationErrors = new ArrayList<>();
    private long lastLoadedAt;

    public ConfigManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        migrationRan = false;
        migratedFrom = -1;
        regeneratedDefaults = false;
        validationErrors.clear();
        ensureDataFolder();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            copyDefaultConfig(configFile);
            regeneratedDefaults = true;
        }

        FileConfiguration configuration = loadAndMigrate(configFile);
        this.settings = Settings.fromYaml(configuration);
        this.loadedVersion = settings.configVersion;
        this.validationErrors = ConfigValidator.validate(settings);
        this.lastLoadedAt = System.currentTimeMillis();
        if (!validationErrors.isEmpty()) {
            plugin.getLogger().warning("[ACAC] Config validation reported: " + validationErrors);
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public int getLoadedVersion() {
        return loadedVersion;
    }

    public boolean isMigrationRan() {
        return migrationRan;
    }

    public int getMigratedFrom() {
        return migratedFrom;
    }

    public boolean isRegeneratedDefaults() {
        return regeneratedDefaults;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public boolean isValid() {
        return validationErrors == null || validationErrors.isEmpty();
    }

    public long getLastLoadedAt() {
        return lastLoadedAt;
    }

    private void ensureDataFolder() {
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[ACAC] Unable to create data folder", ex);
        }
    }

    private void copyDefaultConfig(File configFile) {
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (in != null) {
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                plugin.saveDefaultConfig();
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[ACAC] Failed to write default config", ex);
        }
    }

    private FileConfiguration loadAndMigrate(File configFile) {
        try {
            InputStream resource = plugin.getClass().getClassLoader().getResourceAsStream("config.yml");
            YamlConfiguration defaults = resource != null
                    ? YamlConfiguration.loadConfiguration(new InputStreamReader(resource))
                    : new YamlConfiguration();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.setDefaults(defaults);
            config.options().copyDefaults(true);

            int version = config.getInt("config-version", 1);
            if (version < Settings.CURRENT_CONFIG_VERSION) {
                migrateConfig(configFile.toPath(), config, version);
            } else if (version > Settings.CURRENT_CONFIG_VERSION) {
                plugin.getLogger().warning("[ACAC] Config version " + version + " is newer than supported "
                        + Settings.CURRENT_CONFIG_VERSION + "; loading with caution.");
            }
            return config;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[ACAC] Failed to load YAML config, using in-memory defaults", ex);
            plugin.reloadConfig();
            return plugin.getConfig();
        }
    }

    private void migrateConfig(Path configPath, YamlConfiguration config, int fromVersion) {
        try {
            Path backup = configPath.getParent().resolve("config.yml.bak-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
            Files.copy(configPath, backup, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("[ACAC] Backed up old config to " + backup.getFileName());
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[ACAC] Failed to back up old config", ex);
        }

        // Merge defaults for missing keys
        ConfigurationSection defaults = config.getDefaults();
        if (defaults != null) {
            mergeMissing("", defaults, config);
        }
        config.set("config-version", Settings.CURRENT_CONFIG_VERSION);
        try {
            config.save(configPath.toFile());
            migrationRan = true;
            migratedFrom = fromVersion;
            plugin.getLogger().info("[ACAC] Migrated config.yml from v" + fromVersion + " to v"
                    + Settings.CURRENT_CONFIG_VERSION + ".");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[ACAC] Failed to save migrated config", ex);
        }
    }

    private void mergeMissing(String prefix, ConfigurationSection source, FileConfiguration target) {
        for (String key : source.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = source.get(key);
            if (value instanceof ConfigurationSection section) {
                mergeMissing(path, section, target);
            } else if (!target.contains(path)) {
                target.set(path, value);
            }
        }
    }
}
