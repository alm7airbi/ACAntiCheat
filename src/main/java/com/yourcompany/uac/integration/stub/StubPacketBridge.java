package com.yourcompany.uac.integration.stub;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.integration.bridge.PacketBridge;
import com.yourcompany.uac.packet.PacketInterceptor;

/**
 * Offline stub that simply logs packet interception binding.
 */
public class StubPacketBridge implements PacketBridge {

    private final UltimateAntiCheatPlugin plugin;

    public StubPacketBridge(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(PacketInterceptor interceptor) {
        plugin.getHookManager().registerPacketInterceptor(interceptor);
        plugin.getLogger().info("[ACAC] Using stub packet bridge (offline).");
    }

    @Override
    public String name() {
        return "stub";
    }
}
