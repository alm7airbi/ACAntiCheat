package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Captures rapid-fire entity interactions (placing, spawning, interacting)
 * so entity overload patterns can be detected.
 */
public class EntityActionContext {
    private final Player player;
    private final PlayerCheckState state;
    private final String actionType;
    private final long timestamp;
    private final int recentCount;

    public EntityActionContext(Player player, PlayerCheckState state, String actionType, long timestamp, int recentCount) {
        this.player = player;
        this.state = state;
        this.actionType = actionType;
        this.timestamp = timestamp;
        this.recentCount = recentCount;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public String getActionType() {
        return actionType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getRecentCount() {
        return recentCount;
    }
}
