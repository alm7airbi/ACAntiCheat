package org.bukkit.event.inventory;

import org.bukkit.entity.Player;

import java.util.Set;

public class InventoryDragEvent {
    private final Player who;
    private final Set<Integer> rawSlots;
    private boolean cancelled;

    public InventoryDragEvent(Player who, Set<Integer> rawSlots) {
        this.who = who;
        this.rawSlots = rawSlots;
    }

    public Player getWhoClicked() {
        return who;
    }

    public Set<Integer> getRawSlots() {
        return rawSlots;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
