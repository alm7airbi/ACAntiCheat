package com.comphenix.protocol.events;

// TODO: Replace this stub with the real ProtocolLib PacketEvent for production builds.

import org.bukkit.entity.Player;

public class PacketEvent {
    private final Player player;
    private final Object packet;

    public PacketEvent(Player player, Object packet) {
        this.player = player;
        this.packet = packet;
    }

    public Player getPlayer() {
        return player;
    }

    public Object getPacket() {
        return packet;
    }
}
