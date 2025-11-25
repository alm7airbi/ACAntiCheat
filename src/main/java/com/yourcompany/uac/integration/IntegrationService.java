package com.yourcompany.uac.integration;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.integration.bridge.EventBridge;
import com.yourcompany.uac.integration.bridge.InventoryAccess;
import com.yourcompany.uac.integration.bridge.MitigationActions;
import com.yourcompany.uac.integration.bridge.PacketBridge;
import com.yourcompany.uac.integration.bridge.SoftIntegrationBridge;
import com.yourcompany.uac.integration.paper.PaperEventBridge;
import com.yourcompany.uac.integration.paper.PaperInventoryAccess;
import com.yourcompany.uac.integration.paper.PaperMitigationActions;
import com.yourcompany.uac.integration.paper.PaperPacketBridge;
import com.yourcompany.uac.integration.stub.StubEventBridge;
import com.yourcompany.uac.integration.stub.StubInventoryAccess;
import com.yourcompany.uac.integration.stub.StubMitigationActions;
import com.yourcompany.uac.integration.stub.StubPacketBridge;

/**
 * Chooses between offline stub implementations and real Paper/ProtocolLib
 * bridges. Selection is driven by config (integrations.mode) so local devs can
 * switch to real bindings without touching code.
 */
public class IntegrationService {

    private final PacketBridge packetBridge;
    private final MitigationActions mitigationActions;
    private final InventoryAccess inventoryAccess;
    private final EventBridge eventBridge;
    private final SoftIntegrationBridge softIntegrationBridge;
    private final boolean usingStub;

    public IntegrationService(UltimateAntiCheatPlugin plugin) {
        String mode = plugin.getConfigManager().getSettings() != null
                ? plugin.getConfigManager().getSettings().integrationMode
                : plugin.getConfig().getString("integrations.mode", "stub");
        boolean protocolLibPresent = plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        boolean preferStub = "stub".equalsIgnoreCase(mode) || Boolean.getBoolean("acac.stub");
        boolean prefersPaper = "paper".equalsIgnoreCase(mode) || "auto".equalsIgnoreCase(mode);
        if (preferStub || (!protocolLibPresent && !"paper".equalsIgnoreCase(mode))) {
            packetBridge = new StubPacketBridge(plugin);
            mitigationActions = new StubMitigationActions(plugin);
            inventoryAccess = new StubInventoryAccess();
            eventBridge = new StubEventBridge(plugin);
            softIntegrationBridge = new SoftIntegrationBridge(plugin);
            usingStub = true;
        } else {
            packetBridge = new PaperPacketBridge(plugin);
            mitigationActions = new PaperMitigationActions(plugin);
            inventoryAccess = new PaperInventoryAccess();
            eventBridge = new PaperEventBridge(plugin);
            softIntegrationBridge = new SoftIntegrationBridge(plugin);
            usingStub = false;
            if (!protocolLibPresent && prefersPaper) {
                plugin.getLogger().warning("[ACAC] ProtocolLib not detected; packet listeners will be limited.");
            }
        }
        plugin.getLogger().info("[ACAC] IntegrationService active mode=" + (usingStub ? "stub" : "paper"));
        softIntegrationBridge.logDetectedPresence();
    }

    public PacketBridge getPacketBridge() {
        return packetBridge;
    }

    public MitigationActions getMitigationActions() {
        return mitigationActions;
    }

    public InventoryAccess getInventoryAccess() {
        return inventoryAccess;
    }

    public EventBridge getEventBridge() {
        return eventBridge;
    }

    public SoftIntegrationBridge getSoftIntegrationBridge() {
        return softIntegrationBridge;
    }

    public boolean isUsingStub() {
        return usingStub;
    }

    public String name() {
        return usingStub ? "stub" : "paper";
    }
}
