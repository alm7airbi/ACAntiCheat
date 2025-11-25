package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Represents a player command invocation used for spam/abuse detection.
 */
public class CommandContext {

    private final Player player;
    private final PlayerCheckState state;
    private final String commandLine;
    private final long timestamp;
    private final int windowCount;

    public CommandContext(Player player, PlayerCheckState state, String commandLine, long timestamp, int windowCount) {
        this.player = player;
        this.state = state;
        this.commandLine = commandLine;
        this.timestamp = timestamp;
        this.windowCount = windowCount;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getWindowCount() {
        return windowCount;
    }
}
