package com.yourcompany.uac;

import com.yourcompany.uac.config.ConfigManager;
import com.yourcompany.uac.packet.PacketListenerManager;
import com.yourcompany.uac.storage.DatabaseManager;
import com.yourcompany.uac.integration.ExternalPluginHookManager;
import com.yourcompany.uac.ui.CommandHandler;
import com.yourcompany.uac.ui.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for UltimateAntiCheat. Wires configuration, storage, integrations,
 * and packet listeners. Concrete checks are registered by the packet listener manager
 * after integrations are determined (ProtocolLib, PacketEvents, etc.).
 */
public class UltimateAntiCheatPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PacketListenerManager packetListenerManager;
    private ExternalPluginHookManager hookManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        // Load configuration early
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // Setup persistence for player states, trust scores, and audit logs
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        // Discover and prepare integrations (ProtocolLib / PacketEvents / ViaVersion)
        this.hookManager = new ExternalPluginHookManager(this);
        this.hookManager.init();

        // Packet interception and registration of specific checks
        this.packetListenerManager = new PacketListenerManager(this);
        this.packetListenerManager.registerListeners();

        // Commands + GUI wiring
        this.guiManager = new GuiManager(this);
        if (getCommand("uac") != null) {
            getCommand("uac").setExecutor(new CommandHandler(this));
        }

        getLogger().info("[UAC] UltimateAntiCheat enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[UAC] UltimateAntiCheat disabling…");
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
}
