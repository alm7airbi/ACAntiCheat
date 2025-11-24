package com.yourcompany.uac.packet;

import org.bukkit.entity.Player;

/**
 * Lightweight wrapper for incoming packet data to avoid tying checks to
 * a specific packet library. Packet object is intentionally opaque.
 */
public class PacketPayload {
    private final Player player;
    private final Object rawPacket;

    public PacketPayload(Player player, Object rawPacket) {
        this.player = player;
        this.rawPacket = rawPacket;
    }

    public Player getPlayer() {
        return player;
    }

    public Object getRawPacket() {
        return rawPacket;
    }
}
