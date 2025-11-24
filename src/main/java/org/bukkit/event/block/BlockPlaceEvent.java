package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockPlaceEvent {
    private final Player player;
    private final Block blockPlaced;
    private boolean cancelled;

    public BlockPlaceEvent(Player player, Block blockPlaced) {
        this.player = player;
        this.blockPlaced = blockPlaced;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlockPlaced() {
        return blockPlaced;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
