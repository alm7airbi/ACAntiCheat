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
    private final java.util.Map<Integer, java.util.UUID> slotPlayerLookup = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> toggleLookup = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> mitigationLookup = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, java.util.UUID> inspectTargets = new java.util.HashMap<>();

    public GuiManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.checkManager = plugin.getCheckManager();
    }

    public void openMainGui(CommandSender sender) {
        if (sender instanceof Player player) {
            Inventory inv = Bukkit.createInventory((InventoryHolder) null, 27, "ACAC Control");
            slotPlayerLookup.clear();
            toggleLookup.clear();
            mitigationLookup.clear();
            int index = 0;
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                ItemStack indicator = new ItemStack(Material.STONE);
                var stats = checkManager.getStatsForPlayer(online.getUniqueId());
                indicator.setDisplayName("§b" + online.getName() + " §7(" + String.format("%.1f", stats.trustScore()) + " trust)");
                indicator.setLore(java.util.List.of(
                        "Packets/s: " + String.format("%.1f", stats.packetsPerSecond()),
                        "Flags: " + stats.flagCounts(),
                        "Mitigation: " + stats.lastMitigation(),
                        "Click: Inspect / actions"));
                inv.setItem(index++, indicator);
                slotPlayerLookup.put(index - 1, online.getUniqueId());
            }
            java.util.List<String> toggles = java.util.List.of(
                    "packet-rate-limit.enabled",
                    "invalid-packet.enabled",
                    "invalid-teleport.enabled",
                    "inventory-exploit.enabled",
                    "invalid-placement.enabled",
                    "chunk-crash.enabled",
                    "entity-overload.enabled",
                    "command-abuse.enabled",
                    "sign-payload.enabled",
                    "redstone-exploit.enabled");
            int toggleSlot = 18;
            for (String key : toggles) {
                ItemStack toggle = new ItemStack(Material.DIRT);
                boolean enabled = isEnabled(key);
                toggle.setDisplayName((enabled ? "§a" : "§c") + key);
                toggle.setLore(java.util.List.of("Click to toggle"));
                inv.setItem(toggleSlot, toggle);
                toggleLookup.put(toggleSlot, key);
                toggleSlot++;
            }
            java.util.Map<String, String> mitigations = java.util.Map.of(
                    "PacketRateLimiter", "packet-rate-limit.action",
                    "InvalidPacket", "invalid-packet.action",
                    "InvalidTeleport", "invalid-teleport.action",
                    "InventoryDupeCheck", "inventory-exploit.action",
                    "InvalidPlacementCheck", "invalid-placement.action",
                    "ChunkCrashCheck", "chunk-crash.action"
            );
            int mitSlot = 9;
            for (var entry : mitigations.entrySet()) {
                var current = plugin.getConfigManager().getSettings().getMitigationMode(entry.getKey());
                ItemStack mit = new ItemStack(Material.STONE);
                mit.setDisplayName("§b" + entry.getKey() + " mitigation: " + current);
                mit.setLore(java.util.List.of("Click to cycle"));
                inv.setItem(mitSlot, mit);
                mitigationLookup.put(mitSlot, entry.getKey());
                mitSlot++;
            }
            ItemStack global = new ItemStack(Material.DIRT);
            global.setDisplayName("Global log-only: " + plugin.getMitigationManager().isLogOnly());
            inv.setItem(26, global);
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
            checkManager.findPlayerId(target).ifPresent(uuid -> inspectTargets.put(player.getUniqueId(), uuid));
            ItemStack trust = new ItemStack(Material.STONE);
            checkManager.findPlayerId(target).ifPresent(uuid -> {
                CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(uuid);
                trust.setDisplayName("Trust: " + stats.trustScore());
                trust.setLore(java.util.List.of(
                        "Packets/s: " + String.format("%.1f", stats.packetsPerSecond()),
                        "Flags: " + stats.flagCounts(),
                        "Mitigation: " + stats.lastMitigation()));
            });
            inv.setItem(0, trust);
            ItemStack rubber = new ItemStack(Material.DIRT);
            rubber.setDisplayName("Rubber-band");
            rubber.setLore(java.util.List.of("Send player to last safe position"));
            inv.setItem(7, rubber);
            ItemStack reset = new ItemStack(Material.STONE);
            reset.setDisplayName("Reset trust + flags");
            reset.setLore(java.util.List.of("Clears counters for this player"));
            inv.setItem(8, reset);
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
        if (toggleLookup.containsKey(event.getSlot())) {
            String path = toggleLookup.get(event.getSlot());
            boolean next = toggleSetting(path);
            player.sendMessage("[ACAC] Toggled " + path + " to " + next);
            openMainGui(player);
            return;
        }
        if (mitigationLookup.containsKey(event.getSlot())) {
            String check = mitigationLookup.get(event.getSlot());
            var next = nextMitigation(check);
            player.sendMessage("[ACAC] " + check + " mitigation -> " + next);
            openMainGui(player);
            return;
        }
        if (slotPlayerLookup.containsKey(event.getSlot())) {
            java.util.UUID targetId = slotPlayerLookup.get(event.getSlot());
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.getUniqueId().equals(targetId))
                    .findFirst()
                    .ifPresent(p -> openInspectGui(player, p.getName()));
            return;
        }
        if (title.startsWith("Inspect ")) {
            String target = title.substring("Inspect ".length());
            java.util.UUID uuid = inspectTargets.get(player.getUniqueId());
            if (uuid == null) {
                checkManager.findPlayerId(target).ifPresent(id -> inspectTargets.put(player.getUniqueId(), id));
                uuid = inspectTargets.get(player.getUniqueId());
            }
            if (uuid == null) {
                player.sendMessage("[ACAC] Unknown player " + target);
                return;
            }
            if (event.getSlot() == 7) {
                var state = checkManager.getOrCreateState(uuid);
                plugin.getIntegrationService().getMitigationActions()
                        .rubberBand(player, "GUI", state.getLastKnownPosition(), "Staff rubber-band");
                return;
            }
            if (event.getSlot() == 8) {
                checkManager.resetTrust(uuid);
                checkManager.clearFlags(uuid);
                player.sendMessage("[ACAC] Cleared trust/flags for " + target);
                openInspectGui(player, target);
            }
        }
    }

    private boolean isEnabled(String key) {
        var s = plugin.getConfigManager().getSettings();
        return switch (key) {
            case "packet-rate-limit.enabled" -> s.packetRateLimiterEnabled;
            case "invalid-packet.enabled" -> s.invalidPacketEnabled;
            case "invalid-teleport.enabled" -> s.invalidTeleportEnabled;
            case "inventory-exploit.enabled" -> s.inventoryExploitEnabled;
            case "invalid-placement.enabled" -> s.invalidPlacementEnabled;
            default -> true;
        };
    }

    private boolean toggleSetting(String key) {
        var s = plugin.getConfigManager().getSettings();
        return switch (key) {
            case "packet-rate-limit.enabled" -> s.packetRateLimiterEnabled = !s.packetRateLimiterEnabled;
            case "invalid-packet.enabled" -> s.invalidPacketEnabled = !s.invalidPacketEnabled;
            case "invalid-teleport.enabled" -> s.invalidTeleportEnabled = !s.invalidTeleportEnabled;
            case "inventory-exploit.enabled" -> s.inventoryExploitEnabled = !s.inventoryExploitEnabled;
            case "invalid-placement.enabled" -> s.invalidPlacementEnabled = !s.invalidPlacementEnabled;
            case "chunk-crash.enabled" -> s.chunkCrashEnabled = !s.chunkCrashEnabled;
            case "entity-overload.enabled" -> s.entityOverloadEnabled = !s.entityOverloadEnabled;
            case "command-abuse.enabled" -> s.commandAbuseEnabled = !s.commandAbuseEnabled;
            case "sign-payload.enabled" -> s.signPayloadEnabled = !s.signPayloadEnabled;
            case "redstone-exploit.enabled" -> s.redstoneEnabled = !s.redstoneEnabled;
            default -> true;
        };
    }

    private PlayerCheckState.MitigationLevel nextMitigation(String check) {
        var s = plugin.getConfigManager().getSettings();
        var current = s.getMitigationMode(check);
        PlayerCheckState.MitigationLevel next;
        if (current == null) {
            next = PlayerCheckState.MitigationLevel.NONE;
        } else {
            next = switch (current) {
                case NONE -> PlayerCheckState.MitigationLevel.WARN;
                case WARN -> PlayerCheckState.MitigationLevel.ROLLBACK;
                case ROLLBACK -> PlayerCheckState.MitigationLevel.THROTTLE;
                case THROTTLE -> PlayerCheckState.MitigationLevel.RUBBERBAND;
                case RUBBERBAND -> PlayerCheckState.MitigationLevel.KICK;
                case KICK -> PlayerCheckState.MitigationLevel.TEMP_BAN;
                case TEMP_BAN -> PlayerCheckState.MitigationLevel.PERM_BAN;
                case PERM_BAN -> PlayerCheckState.MitigationLevel.NONE;
            };
        }
        s.mitigationModes.put(check, next);
        return next;
    }

    private ItemStack labelled(Material type, String name, java.util.List<String> lore) {
        ItemStack stack = new ItemStack(type);
        stack.setDisplayName(name);
        stack.setLore(lore);
        return stack;
    }
}
