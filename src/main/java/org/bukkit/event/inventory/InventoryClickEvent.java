package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryClickEvent {
    private final Player whoClicked;
    private final ItemStack currentItem;

    public InventoryClickEvent(Player whoClicked, ItemStack currentItem) {
        this.whoClicked = whoClicked;
        this.currentItem = currentItem;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public Player getWhoClicked() {
        return whoClicked;
    }
}
