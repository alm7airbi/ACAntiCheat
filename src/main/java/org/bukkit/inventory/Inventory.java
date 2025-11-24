package org.bukkit.inventory;

public class Inventory {
    private final InventoryHolder holder;
    private final int size;
    private final String title;

    public Inventory(InventoryHolder holder, int size, String title) {
        this.holder = holder;
        this.size = size;
        this.title = title;
    }

    public InventoryHolder getHolder() {
        return holder;
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }
}
