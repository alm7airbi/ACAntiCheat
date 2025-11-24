package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Inventory click/drag metadata. In a real server this would be populated from
 * InventoryClickEvent/InventoryDragEvent.
 */
public class InventoryActionContext {
    private final Player player;
    private final PlayerCheckState state;
    private final String actionType;
    private final int slot;
    private final Object item;
    private final long timestamp;
    private final int windowCount;

    public InventoryActionContext(Player player, PlayerCheckState state, String actionType, int slot, Object item,
                                  long timestamp, int windowCount) {
        this.player = player;
        this.state = state;
        this.actionType = actionType;
        this.slot = slot;
        this.item = item;
        this.timestamp = timestamp;
        this.windowCount = windowCount;
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

    public int getSlot() {
        return slot;
    }

    public Object getItem() {
        return item;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getWindowCount() {
        return windowCount;
    }
}
