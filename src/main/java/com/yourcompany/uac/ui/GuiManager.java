package com.yourcompany.uac.ui;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Material;

/**
 * Central place to open inventory-based GUIs for staff.
 */
public class GuiManager {

    private final UltimateAntiCheatPlugin plugin;
    private final CheckManager checkManager;

    public GuiManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.checkManager = plugin.getCheckManager();
    }

    public void openMainGui(CommandSender sender) {
        if (sender instanceof Player player) {
            Inventory inv = Bukkit.createInventory((InventoryHolder) null, 27, "ACAC Control");
            int index = 0;
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                ItemStack indicator = new ItemStack(Material.STONE);
                inv.setItem(index++, indicator);
            }
            ItemStack toggle = new ItemStack(Material.DIRT);
            inv.setItem(26, toggle);
            player.openInventory(inv);
            player.sendMessage("[ACAC] Click items to toggle mitigation or inspect players.");
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

    public void openInspectGui(CommandSender sender, String target) {
        if (sender instanceof Player player) {
            Inventory inv = Bukkit.createInventory((InventoryHolder) null, 27, "Inspect " + target);
            // TODO: draw per-check stats and mitigation buttons in real Paper GUI.
            player.openInventory(inv);
        } else {
            sender.sendMessage("[UAC] Inspect GUI requires a player; showing text view instead.");
            checkManager.findPlayerId(target).ifPresent(uuid -> {
                CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(uuid);
                sender.sendMessage("[UAC] text inspect for " + target + " trust=" + stats.trustScore());
            });
        }
    }

    public void handleClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory == null || event.getWhoClicked() == null) {
            return;
        }
        String title = inventory.getTitle();
        if (title == null || !title.startsWith("ACAC")) {
            return;
        }
        Player player = event.getWhoClicked();
        if ("ACAC Control".equals(title) && event.getSlot() == 26) {
            boolean next = !plugin.getMitigationManager().isLogOnly();
            plugin.getMitigationManager().setLogOnly(next);
            player.sendMessage("[ACAC] Global mitigation log-only=" + next);
            return;
        }
        if (title.startsWith("Inspect ")) {
            String target = title.substring("Inspect ".length());
            checkManager.findPlayerId(target).ifPresent(uuid -> {
                CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(uuid);
                PlayerCheckState.MitigationLevel newLevel = PlayerCheckState.MitigationLevel.values()[Math.min(PlayerCheckState.MitigationLevel.values().length - 1, stats.lastMitigation().ordinal() + 1)];
                player.sendMessage("[ACAC] Would escalate " + target + " to " + newLevel + " based on click.");
            });
        }
    }
}
