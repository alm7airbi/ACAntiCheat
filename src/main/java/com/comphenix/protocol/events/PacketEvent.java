package com.comphenix.protocol.events;

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
