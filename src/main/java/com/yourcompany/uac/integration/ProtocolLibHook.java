package com.yourcompany.uac.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.packet.PacketInterceptor;

/**
 * Bridges ProtocolLib packet events to the plugin's internal interceptor.
 */
public class ProtocolLibHook {

    private final UltimateAntiCheatPlugin plugin;
    private ProtocolManager protocolManager;

    public ProtocolLibHook(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void bind(PacketInterceptor interceptor) {
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.getInstance()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                interceptor.handleIncoming(event.getPlayer(), event.getPacket());
            }
        });
    }
}
