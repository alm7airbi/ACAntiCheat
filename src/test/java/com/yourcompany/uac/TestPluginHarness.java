package com.yourcompany.uac;

import com.yourcompany.uac.config.ConfigManager;
import com.yourcompany.uac.config.Settings;
import com.yourcompany.uac.integration.IntegrationService;
import com.yourcompany.uac.mitigation.AlertManager;
import com.yourcompany.uac.mitigation.MitigationManager;
import com.yourcompany.uac.storage.DatabaseManager;
import com.yourcompany.uac.ui.GuiManager;
import com.yourcompany.uac.util.TrustScoreManager;

import java.io.File;

/**
 * Lightweight plugin harness for unit tests that need real wiring without
 * spinning up a Paper server. It injects in-memory settings and isolates the
 * data folder to a temporary directory per test run.
 */
public class TestPluginHarness extends UltimateAntiCheatPlugin {

    private final File dataFolder;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final TrustScoreManager trustScoreManager;
    private final IntegrationService integrationService;
    private final MitigationManager mitigationManager;
    private final AlertManager alertManager;
    private final com.yourcompany.uac.checks.CheckManager checkManager;
    private final GuiManager guiManager;

    public TestPluginHarness(Settings settings, File dataFolder) {
        this.dataFolder = dataFolder;
        this.dataFolder.mkdirs();
        this.configManager = new InMemoryConfigManager(this, settings);
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();
        this.trustScoreManager = new TrustScoreManager();
        this.integrationService = new IntegrationService(this);
        this.mitigationManager = new MitigationManager(this, integrationService.getMitigationActions());
        this.alertManager = new AlertManager(this);
        this.checkManager = new com.yourcompany.uac.checks.CheckManager(this, trustScoreManager, mitigationManager, alertManager, databaseManager);
        this.guiManager = new GuiManager(this);
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    public TrustScoreManager getTrustScoreManager() {
        return trustScoreManager;
    }

    @Override
    public IntegrationService getIntegrationService() {
        return integrationService;
    }

    @Override
    public MitigationManager getMitigationManager() {
        return mitigationManager;
    }

    @Override
    public AlertManager getAlertManager() {
        return alertManager;
    }

    @Override
    public com.yourcompany.uac.checks.CheckManager getCheckManager() {
        return checkManager;
    }

    @Override
    public GuiManager getGuiManager() {
        return guiManager;
    }

    public void shutdown() {
        alertManager.shutdown();
        databaseManager.disconnect();
    }

    private static class InMemoryConfigManager extends ConfigManager {
        private final Settings settings;

        InMemoryConfigManager(UltimateAntiCheatPlugin plugin, Settings settings) {
            super(plugin);
            this.settings = settings;
        }

        @Override
        public void load() {
            // In-memory settings only; no file IO.
        }

        @Override
        public Settings getSettings() {
            return settings;
        }

        @Override
        public int getLoadedVersion() {
            return settings.configVersion;
        }

        @Override
        public boolean isMigrationRan() {
            return false;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}

