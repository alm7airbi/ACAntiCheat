package org.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class Bukkit {
    private Bukkit() {}

    private static final Server SERVER = new Server();

    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        return new Inventory(holder, size, title);
    }

    public static Server getServer() {
        return SERVER;
    }
}
