package com.yourcompany.uac.integration;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.packet.PacketInterceptor;
import org.bukkit.plugin.PluginManager;

/**
 * Detects presence of ecosystem plugins and binds interception accordingly.
 */
public class ExternalPluginHookManager {

    private final UltimateAntiCheatPlugin plugin;
    private ProtocolLibHook protocolLibHook;
    private PacketEventsHook packetEventsHook;

    public ExternalPluginHookManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (pm.isPluginEnabled("ProtocolLib")) {
            protocolLibHook = new ProtocolLibHook(plugin);
            plugin.getLogger().info("[UAC] ProtocolLib detected, enabling packet bridge.");
        }
        if (pm.isPluginEnabled("packetevents")) {
            packetEventsHook = new PacketEventsHook(plugin);
            plugin.getLogger().info("[UAC] PacketEvents detected, enabling packet bridge.");
        }
    }

    public void registerPacketInterceptor(PacketInterceptor interceptor) {
        if (protocolLibHook != null) {
            protocolLibHook.bind(interceptor);
        } else if (packetEventsHook != null) {
            packetEventsHook.bind(interceptor);
        } else {
            plugin.getLogger().warning("[UAC] No packet library found; packet-level checks disabled.");
        }
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

    public PacketEventsHook getPacketEventsHook() {
        return packetEventsHook;
    }
}
