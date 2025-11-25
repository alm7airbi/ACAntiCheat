package com.yourcompany.uac.config;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigManagerTest {

    @Test
    void loadsAndValidatesConfig() throws Exception {
        UltimateAntiCheatPlugin plugin = new UltimateAntiCheatPlugin() {
            private final File folder = Files.createTempDirectory("acac-config-valid").toFile();
            @Override public File getDataFolder() { return folder; }
        };

        ConfigManager manager = new ConfigManager(plugin);
        manager.load();

        assertTrue(manager.isValid(), "default config should validate");
        assertEquals(Settings.CURRENT_CONFIG_VERSION, manager.getLoadedVersion());
        assertTrue(manager.getSettings().experimentsEnabled, "experiments should default to enabled");
        assertFalse(manager.getSettings().experimentsLogFile.isEmpty(), "experiment log file should be set");
    }

    @Test
    void flagsInvalidValues() throws Exception {
        File tempDir = Files.createTempDirectory("acac-config-invalid").toFile();
        File configFile = new File(tempDir, "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("config-version: 1\n");
        }

        UltimateAntiCheatPlugin plugin = new UltimateAntiCheatPlugin() {
            @Override public File getDataFolder() { return tempDir; }
        };

        ConfigManager manager = new ConfigManager(plugin);
        manager.load();
        assertTrue(manager.isMigrationRan(), "older configs should be migrated");
        assertEquals(Settings.CURRENT_CONFIG_VERSION, manager.getLoadedVersion());
    }

    @Test
    void migratesOlderConfigVersion() throws Exception {
        File tempDir = Files.createTempDirectory("acac-config-migrate").toFile();
        File configFile = new File(tempDir, "config.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("config-version", 1);
        cfg.set("checks.packet-rate-limit.per-second", 5);
        cfg.save(configFile);

        UltimateAntiCheatPlugin plugin = new UltimateAntiCheatPlugin() {
            @Override public File getDataFolder() { return tempDir; }
        };

        ConfigManager manager = new ConfigManager(plugin);
        manager.load();
        assertTrue(manager.isMigrationRan(), "older configs should migrate");
        assertEquals(Settings.CURRENT_CONFIG_VERSION, manager.getLoadedVersion());
    }
}

