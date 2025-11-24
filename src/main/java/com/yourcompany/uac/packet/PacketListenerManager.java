package com.yourcompany.uac.packet;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.checktypes.PacketRateLimiterCheck;
import com.yourcompany.uac.integration.ProtocolLibHook;

/**
 * Registers packet interception according to available libraries.
 */
public class PacketListenerManager {

    private final UltimateAntiCheatPlugin plugin;

    public PacketListenerManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PacketInterceptor interceptor = new PacketInterceptor(plugin);
        plugin.getHookManager().registerPacketInterceptor(interceptor);

        // Register built-in packet checks (rate limiter, invalid packets, etc.)
        interceptor.registerCheck(new PacketRateLimiterCheck(plugin));
        // Additional checks (InvalidPacketCheck, NettyCrashProtectionCheck) would be added here.

        // ProtocolLib example registration
        if (plugin.getHookManager().getProtocolLibHook() != null) {
            ProtocolLibHook protocolLibHook = plugin.getHookManager().getProtocolLibHook();
            protocolLibHook.bind(interceptor);
        }
    }
}
