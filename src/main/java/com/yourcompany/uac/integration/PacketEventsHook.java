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
        // TODO: wire PacketEvents listeners and forward to interceptor
        plugin.getLogger().info("[UAC] PacketEvents binding stubbed - implement listener forwarding.");
    }
}
