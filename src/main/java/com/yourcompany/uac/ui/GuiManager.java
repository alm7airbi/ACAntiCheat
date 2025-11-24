package com.yourcompany.uac.ui;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.Bukkit;

/**
 * Central place to open inventory-based GUIs for staff.
 */
public class GuiManager {

    private final UltimateAntiCheatPlugin plugin;

    public GuiManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainGui(CommandSender sender) {
        if (sender instanceof Player player) {
            Inventory inv = Bukkit.createInventory((InventoryHolder) null, 27, "UAC Control");
            player.openInventory(inv);
        } else {
            sender.sendMessage("Main GUI requires a player.");
        }
    }

    public void openStatsGui(CommandSender sender) {
        sender.sendMessage("[UAC] Stats GUI placeholder");
    }

    public void openConfigGui(CommandSender sender) {
        sender.sendMessage("[UAC] Config GUI placeholder");
    }
}
