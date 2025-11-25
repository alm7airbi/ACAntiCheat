package com.comphenix.protocol.events;

// Stub-only PacketAdapter so offline builds compile; real ProtocolLib replaces this in Paper mode.

import com.comphenix.protocol.PacketType;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PacketAdapter {
    protected final JavaPlugin plugin;
    protected final PacketType[] packetTypes;

    protected PacketAdapter(JavaPlugin plugin, PacketType... packetTypes) {
        this.plugin = plugin;
        this.packetTypes = packetTypes;
    }

    public void onPacketReceiving(PacketEvent event) {
        // default no-op
    }

    public PacketType[] getPacketTypes() {
        return packetTypes;
    }
}
