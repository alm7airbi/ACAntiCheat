package org.bukkit.event.player;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PlayerInteractEvent {
    private final Player player;
    private final Action action;
    private final Block clickedBlock;
    private boolean cancelled;

    public PlayerInteractEvent(Player player, Action action, Block clickedBlock) {
        this.player = player;
        this.action = action;
        this.clickedBlock = clickedBlock;
    }

    public Player getPlayer() {
        return player;
    }

    public Action getAction() {
        return action;
    }

    public Block getClickedBlock() {
        return clickedBlock;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Action {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
        LEFT_CLICK_AIR,
        RIGHT_CLICK_AIR,
        PHYSICAL
    }
}
