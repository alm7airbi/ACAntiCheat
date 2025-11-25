package com.comphenix.protocol.events;

// TODO: Replace this stub with the real ProtocolLib PacketEvent for production builds.

import com.comphenix.protocol.PacketType;
import org.bukkit.entity.Player;

public class PacketEvent {
    private final Player player;
    private final Object packet;
    private final PacketType packetType;
    private boolean cancelled;

    public PacketEvent(Player player, Object packet) {
        this(player, packet, PacketType.Play.Client.getInstance());
    }

    public PacketEvent(Player player, Object packet, PacketType packetType) {
        this.player = player;
        this.packet = packet;
        this.packetType = packetType;
    }

    public Player getPlayer() {
        return player;
    }

    public Object getPacket() {
        return packet;
    }

    public PacketType getPacketType() {
        return packetType;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
