package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Represents console/log spam that can be attributed back to a player action.
 */
public class ConsoleMessageContext {
    private final Player player;
    private final PlayerCheckState state;
    private final String message;
    private final long timestamp;
    private final int recentCount;

    public ConsoleMessageContext(Player player, PlayerCheckState state, String message, long timestamp, int recentCount) {
        this.player = player;
        this.state = state;
        this.message = message;
        this.timestamp = timestamp;
        this.recentCount = recentCount;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getRecentCount() {
        return recentCount;
    }
}
