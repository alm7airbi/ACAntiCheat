package com.comphenix.protocol.events;

// TODO: Swap this stub for the real ProtocolLib PacketAdapter when adding the dependency.

import com.comphenix.protocol.PacketType;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PacketAdapter {
    protected final JavaPlugin plugin;
    protected final PacketType packetType;

    protected PacketAdapter(JavaPlugin plugin, PacketType packetType) {
        this.plugin = plugin;
        this.packetType = packetType;
    }

    public void onPacketReceiving(PacketEvent event) {
        // default no-op
    }
}
