package org.bukkit.event.inventory;

import org.bukkit.entity.Player;

import java.util.Set;

public class InventoryDragEvent {
    private final Player who;
    private final Set<Integer> rawSlots;
    private final org.bukkit.inventory.Inventory inventory;
    private boolean cancelled;

    public InventoryDragEvent(Player who, Set<Integer> rawSlots) {
        this(who, rawSlots, null);
    }

    public InventoryDragEvent(Player who, Set<Integer> rawSlots, org.bukkit.inventory.Inventory inventory) {
        this.who = who;
        this.rawSlots = rawSlots;
        this.inventory = inventory;
    }

    public Player getWhoClicked() {
        return who;
    }

    public Set<Integer> getRawSlots() {
        return rawSlots;
    }

    public org.bukkit.inventory.Inventory getInventory() {
        return inventory;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
