package org.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.command.CommandSender;

public final class Bukkit {
    private Bukkit() {}

    private static final Server SERVER = new Server();

    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        return new Inventory(holder, size, title);
    }

    public static Server getServer() {
        return SERVER;
    }

    public static boolean isPrimaryThread() {
        return true;
    }

    public static boolean dispatchCommand(CommandSender sender, String commandLine) {
        return true;
    }
}
