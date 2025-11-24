package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Captures bursts of redstone updates attributed to a player. The real
 * implementation should be backed by Paper's redstone events or region
 * profiling.
 */
public class RedstoneContext {
    private final Player player;
    private final PlayerCheckState state;
    private final int updates;
    private final long timestamp;
    private final PlayerCheckState.Position position;

    public RedstoneContext(Player player, PlayerCheckState state, int updates, long timestamp,
                           PlayerCheckState.Position position) {
        this.player = player;
        this.state = state;
        this.updates = updates;
        this.timestamp = timestamp;
        this.position = position;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public int getUpdates() {
        return updates;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PlayerCheckState.Position getPosition() {
        return position;
    }
}
