package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Block placement/break context. Real implementations should carry the block
 * location, material, and whether the server allowed the interaction.
 */
public class PlacementContext {
    private final Player player;
    private final PlayerCheckState state;
    private final String material;
    private final PlayerCheckState.Position position;
    private final long timestamp;
    private final int windowCount;

    public PlacementContext(Player player, PlayerCheckState state, String material, PlayerCheckState.Position position,
                            long timestamp, int windowCount) {
        this.player = player;
        this.state = state;
        this.material = material;
        this.position = position;
        this.timestamp = timestamp;
        this.windowCount = windowCount;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public String getMaterial() {
        return material;
    }

    public PlayerCheckState.Position getPosition() {
        return position;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getWindowCount() {
        return windowCount;
    }
}
