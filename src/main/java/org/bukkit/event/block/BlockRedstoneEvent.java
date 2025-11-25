package org.bukkit.event.block;

import org.bukkit.block.Block;

public class BlockRedstoneEvent {
    private final Block block;
    private final int newCurrent;
    private boolean cancelled;

    public BlockRedstoneEvent(Block block, int newCurrent) {
        this.block = block;
        this.newCurrent = newCurrent;
    }

    public Block getBlock() {
        return block;
    }

    public int getNewCurrent() {
        return newCurrent;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
