package com.yourcompany.uac.integration;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.packet.PacketInterceptor;

/**
 * Placeholder binding for PacketEvents users.
 */
public class PacketEventsHook {

    private final UltimateAntiCheatPlugin plugin;

    public PacketEventsHook(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void bind(PacketInterceptor interceptor) {
        plugin.getLogger().warning("[UAC] PacketEvents support is currently UNSUPPORTED; please use ProtocolLib mode.");
    }
}
