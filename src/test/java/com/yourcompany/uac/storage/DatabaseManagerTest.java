package com.yourcompany.uac.storage;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.config.Settings;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseManagerTest {

    @Test
    void fallsBackWhenSqlUnavailable() throws Exception {
        Settings settings = Settings.fromYaml(new org.bukkit.configuration.file.YamlConfiguration());
        settings.useSqlDatabase = true;
        settings.sqlUrl = "jdbc:unknown://localhost:0/bogus";
        settings.sqlMaxRetries = 1;
        settings.sqlRetryDelayMillis = 10;
        settings.configVersion = Settings.CURRENT_CONFIG_VERSION;

        File folder = Files.createTempDirectory("acac-db-fallback").toFile();

        UltimateAntiCheatPlugin plugin = new UltimateAntiCheatPlugin() {
            @Override public com.yourcompany.uac.config.ConfigManager getConfigManager() { return new StubConfigManager(this, settings); }
            @Override public File getDataFolder() { return folder; }
        };

        DatabaseManager manager = new DatabaseManager(plugin);
        manager.connect();
        assertTrue(manager.getPersistenceStatus().contains("flat-file"), "invalid SQL should fall back to flat-file");
        manager.disconnect();
    }

    @Test
    void savesAndLoadsSnapshots() throws Exception {
        Settings settings = Settings.fromYaml(new org.bukkit.configuration.file.YamlConfiguration());
        settings.configVersion = Settings.CURRENT_CONFIG_VERSION;
        File folder = Files.createTempDirectory("acac-db-snapshot").toFile();

        UltimateAntiCheatPlugin plugin = new UltimateAntiCheatPlugin() {
            @Override public com.yourcompany.uac.config.ConfigManager getConfigManager() { return new StubConfigManager(this, settings); }
            @Override public File getDataFolder() { return folder; }
        };

        DatabaseManager manager = new DatabaseManager(plugin);
        manager.connect();
        PlayerCheckState state = new PlayerCheckState(UUID.randomUUID());
        state.recordFlag("Test", "reason", 2, System.currentTimeMillis());
        manager.saveSnapshot(state);
        manager.disconnect();
        manager.connect();

        Optional<PlayerSnapshot> snapshot = manager.loadSnapshot(state.getPlayerId());
        assertTrue(snapshot.isPresent(), "snapshot should round-trip");
        assertEquals(state.getFlagCounts(), snapshot.get().flagCounts());
        manager.disconnect();
    }

    private static class StubConfigManager extends com.yourcompany.uac.config.ConfigManager {
        private final Settings settings;
        StubConfigManager(UltimateAntiCheatPlugin plugin, Settings settings) { super(plugin); this.settings = settings; }
        @Override public void load() {}
        @Override public Settings getSettings() { return settings; }
        @Override public int getLoadedVersion() { return settings.configVersion; }
        @Override public boolean isValid() { return true; }
    }
}

