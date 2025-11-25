package com.yourcompany.uac;

import com.yourcompany.uac.config.ConfigManager;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.packet.PacketListenerManager;
import com.yourcompany.uac.storage.DatabaseManager;
import com.yourcompany.uac.integration.ExternalPluginHookManager;
import com.yourcompany.uac.integration.IntegrationService;
import com.yourcompany.uac.ui.CommandHandler;
import com.yourcompany.uac.ui.GuiManager;
import com.yourcompany.uac.util.TrustScoreManager;
import com.yourcompany.uac.mitigation.AlertManager;
import com.yourcompany.uac.mitigation.MitigationManager;
import com.yourcompany.uac.metrics.ExperimentLogger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for UltimateAntiCheat. Wires configuration, storage, integrations,
 * and packet listeners. Concrete checks are registered by the packet listener manager
 * after integrations are determined (ProtocolLib, PacketEvents, etc.).
 */
public class UltimateAntiCheatPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TrustScoreManager trustScoreManager;
    private CheckManager checkManager;
    private PacketListenerManager packetListenerManager;
    private ExternalPluginHookManager hookManager;
    private GuiManager guiManager;
    private IntegrationService integrationService;
    private MitigationManager mitigationManager;
    private AlertManager alertManager;
    private ExperimentLogger experimentLogger;

    @Override
    public void onEnable() {
        // Load configuration early
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // Setup persistence for player states, trust scores, and audit logs
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        this.trustScoreManager = new TrustScoreManager();
        this.integrationService = new IntegrationService(this);
        this.experimentLogger = new ExperimentLogger(this);
        this.mitigationManager = new MitigationManager(this, integrationService.getMitigationActions());
        this.alertManager = new AlertManager(this);
        this.checkManager = new CheckManager(this, trustScoreManager, mitigationManager, alertManager, databaseManager);
        if (integrationService.isUsingStub()) {
            getLogger().warning("[UAC] Running with stub integrations; switch integrations.mode to paper on a real server with ProtocolLib installed.");
        } else {
            getLogger().info("[UAC] Real Paper/ProtocolLib integrations active.");
        }

        // Discover and prepare integrations (ProtocolLib / PacketEvents / ViaVersion)
        this.hookManager = new ExternalPluginHookManager(this);
        this.hookManager.init();

        // Packet interception and registration of specific checks
        this.packetListenerManager = new PacketListenerManager(this, checkManager, integrationService);
        this.packetListenerManager.registerListeners();

        // Commands + GUI wiring
        this.guiManager = new GuiManager(this);
        if (getCommand("acac") != null) {
            getCommand("acac").setExecutor(new CommandHandler(this, checkManager));
        }

        getLogger().info("[UAC] UltimateAntiCheat enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[UAC] UltimateAntiCheat disabling…");
        if (this.alertManager != null) {
            this.alertManager.shutdown();
        }
        if (this.experimentLogger != null) {
            this.experimentLogger.shutdown();
        }
        this.databaseManager.disconnect();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PacketListenerManager getPacketListenerManager() {
        return packetListenerManager;
    }

    public ExternalPluginHookManager getHookManager() {
        return hookManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public TrustScoreManager getTrustScoreManager() {
        return trustScoreManager;
    }

    public IntegrationService getIntegrationService() {
        return integrationService;
    }

    public MitigationManager getMitigationManager() {
        return mitigationManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public ExperimentLogger getExperimentLogger() {
        return experimentLogger;
    }
}
