package com.yourcompany.uac.integration.stub;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.integration.bridge.EventBridge;

/**
 * No-op event bridge for offline builds.
 */
public class StubEventBridge implements EventBridge {

    private final UltimateAntiCheatPlugin plugin;

    public StubEventBridge(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(CheckManager checkManager) {
        plugin.getLogger().info("[ACAC] StubEventBridge active (no Bukkit events wired).");
    }

    @Override
    public String name() {
        return "stub";
    }
}
