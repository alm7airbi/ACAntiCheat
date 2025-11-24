package org.bukkit.entity;

// TODO: Replace this stub with the real org.bukkit.entity.Player from Paper/Spigot.

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class Player implements CommandSender {
    private final String name;
    private final UUID uniqueId;
    private Location location = new Location(0, 0, 0);

    public Player(String name, UUID uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public void openInventory(Inventory inventory) {
        // no-op stub
    }

    public void kickPlayer(String message) {
        // no-op stub for offline builds
    }

    public void teleport(org.bukkit.Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public boolean hasPermission(String permission) {
        return true;
    }

    public void closeInventory() {
        // no-op stub
    }

    @Override
    public void sendMessage(String message) {
        // no-op stub
    }
}
