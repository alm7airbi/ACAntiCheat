package org.bukkit.entity;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class Player implements CommandSender {
    private final String name;
    private final UUID uniqueId;

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

    @Override
    public void sendMessage(String message) {
        // no-op stub
    }
}
