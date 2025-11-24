package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class SignChangeEvent {
    private final Player player;
    private final Block block;
    private final String[] lines;
    private boolean cancelled;

    public SignChangeEvent(Player player, Block block, String[] lines) {
        this.player = player;
        this.block = block;
        this.lines = lines;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlock() {
        return block;
    }

    public String[] getLines() {
        return lines;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
