package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Packet metadata passed to checks so they can access shared player state
 * and derived counters without reaching into maps on their own.
 */
public class PacketContext {
    private final Player player;
    private final Object rawPacket;
    private final PlayerCheckState state;
    private final long timestamp;
    private final int packetsLastSecond;
    private final int packetsLastFiveSeconds;

    public PacketContext(Player player, Object rawPacket, PlayerCheckState state, long timestamp,
                         int packetsLastSecond, int packetsLastFiveSeconds) {
        this.player = player;
        this.rawPacket = rawPacket;
        this.state = state;
        this.timestamp = timestamp;
        this.packetsLastSecond = packetsLastSecond;
        this.packetsLastFiveSeconds = packetsLastFiveSeconds;
    }

    public Player getPlayer() {
        return player;
    }

    public Object getRawPacket() {
        return rawPacket;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPacketsLastSecond() {
        return packetsLastSecond;
    }

    public int getPacketsLastFiveSeconds() {
        return packetsLastFiveSeconds;
    }
}
