package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Represents a movement or position update derived from a packet or event.
 */
public class MovementContext extends PacketContext {

    private final double x;
    private final double y;
    private final double z;
    private final boolean onGround;
    private final boolean serverTeleport;

    public MovementContext(Player player, Object rawPacket, PlayerCheckState state, long timestamp,
                           int packetsLastSecond, int packetsLastFiveSeconds,
                           double x, double y, double z, boolean onGround, boolean serverTeleport) {
        super(player, rawPacket, state, timestamp, packetsLastSecond, packetsLastFiveSeconds);
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
        this.serverTeleport = serverTeleport;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isServerTeleport() {
        return serverTeleport;
    }
}
