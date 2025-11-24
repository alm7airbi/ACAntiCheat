package org.bukkit.inventory;

public class Inventory {
    private final InventoryHolder holder;
    private final int size;
    private final String title;
    private final java.util.Map<Integer, ItemStack> contents = new java.util.HashMap<>();

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

    public void setItem(int slot, ItemStack stack) {
        contents.put(slot, stack);
    }

    public ItemStack getItem(int slot) {
        return contents.get(slot);
    }

    public java.util.Map<Integer, ItemStack> getContents() {
        return java.util.Collections.unmodifiableMap(contents);
    }
}
