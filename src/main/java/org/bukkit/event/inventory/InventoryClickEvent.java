package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryClickEvent {
    private final Player whoClicked;
    private final ItemStack currentItem;
    private final int slot;
    private final org.bukkit.inventory.Inventory inventory;
    private boolean cancelled;

    public InventoryClickEvent(Player whoClicked, ItemStack currentItem) {
        this(whoClicked, currentItem, 0, null);
    }

    public InventoryClickEvent(Player whoClicked, ItemStack currentItem, int slot) {
        this(whoClicked, currentItem, slot, null);
    }

    public InventoryClickEvent(Player whoClicked, ItemStack currentItem, int slot, org.bukkit.inventory.Inventory inventory) {
        this.whoClicked = whoClicked;
        this.currentItem = currentItem;
        this.slot = slot;
        this.inventory = inventory;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public Player getWhoClicked() {
        return whoClicked;
    }

    public int getSlot() {
        return slot;
    }

    public org.bukkit.inventory.Inventory getInventory() {
        return inventory;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
