package com.yourcompany.uac.ui;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central place to open inventory-based GUIs for staff. Supports pagination,
 * filtering, and live data pulled from CheckManager, integration services, and
 * persistence.
 */
public class GuiManager {

    private static final int PLAYER_SLOTS = 27;
    private static final int MAIN_SIZE = 54;
    private static final int ACTION_COOLDOWN_MS = 1_500;
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");

    private final UltimateAntiCheatPlugin plugin;
    private final CheckManager checkManager;
    private final Map<UUID, GuiState> viewerState = new HashMap<>();
    private final Map<UUID, UUID> inspectTargets = new HashMap<>();

    private record GuiState(Map<Integer, UUID> playerSlots,
                            Map<Integer, String> toggleSlots,
                            Map<Integer, String> mitigationSlots,
                            int page,
                            boolean highRiskOnly,
                            long lastActionAt) {
        GuiState withPage(int page) {
            return new GuiState(playerSlots, toggleSlots, mitigationSlots, page, highRiskOnly, lastActionAt);
        }

        GuiState withFilter(boolean highRiskOnly) {
            return new GuiState(playerSlots, toggleSlots, mitigationSlots, page, highRiskOnly, lastActionAt);
        }

        GuiState touch(long now) {
            return new GuiState(playerSlots, toggleSlots, mitigationSlots, page, highRiskOnly, now);
        }
    }

    public GuiManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.checkManager = plugin.getCheckManager();
    }

    public void openMainGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Main GUI requires a player.");
            return;
        }
        GuiState state = viewerState.computeIfAbsent(player.getUniqueId(), id -> new GuiState(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, false, 0));
        List<Player> allPlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        allPlayers.sort(Comparator.comparingDouble(p -> checkManager.getStatsForPlayer(p.getUniqueId()).trustScore()));
        List<Player> filtered = new ArrayList<>();
        for (Player online : allPlayers) {
            var stats = checkManager.getStatsForPlayer(online.getUniqueId());
            boolean highRisk = stats.trustScore() < 60 || stats.underMitigation() || stats.flagCounts().values().stream().mapToInt(Integer::intValue).sum() >= 3;
            if (!state.highRiskOnly || highRisk) {
                filtered.add(online);
            }
        }

        int pages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PLAYER_SLOTS));
        int page = Math.min(state.page, pages - 1);
        int startIndex = page * PLAYER_SLOTS;
        int endIndex = Math.min(filtered.size(), startIndex + PLAYER_SLOTS);

        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, ChatColor.GREEN + "ACAC Control (" + (page + 1) + "/" + pages + ")");
        Map<Integer, UUID> playerSlots = new HashMap<>();
        Map<Integer, String> toggleSlots = new HashMap<>();
        Map<Integer, String> mitigationSlots = new HashMap<>();

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Player online = filtered.get(i);
            var stats = checkManager.getStatsForPlayer(online.getUniqueId());
            ItemStack indicator = labelled(Material.STONE, "§b" + online.getName(), List.of(
                    "Trust: " + ONE_DECIMAL.format(stats.trustScore()) + "/100",
                    "Packets/s: " + ONE_DECIMAL.format(stats.packetsPerSecond()),
                    "Flags: " + stats.flagCounts(),
                    "Mitigation: " + stats.lastMitigation(),
                    "Click: Inspect / actions"));
            inv.setItem(slot, indicator);
            playerSlots.put(slot, online.getUniqueId());
            slot++;
        }

        inv.setItem(27, labelled(Material.ARROW, "Previous page", List.of("Page " + (page + 1) + " of " + pages)));
        inv.setItem(28, labelled(Material.ARROW, "Next page", List.of("Page " + (page + 1) + " of " + pages)));
        inv.setItem(29, labelled(Material.DIRT, state.highRiskOnly ? "Filter: High risk" : "Filter: All", List.of("Toggle high-risk filter")));
        inv.setItem(30, labelled(Material.STONE, "Refresh", List.of("Reload live data")));
        inv.setItem(31, labelled(Material.DIRT, "Global log-only: " + plugin.getMitigationManager().isLogOnly(), List.of("Toggle mitigation log-only")));

        List<String> toggles = List.of(
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
        slot = 32;
        for (String key : toggles) {
            boolean enabled = isEnabled(key);
            inv.setItem(slot, labelled(enabled ? Material.STONE : Material.DIRT, (enabled ? "§a" : "§c") + key, List.of("Click to toggle")));
            toggleSlots.put(slot, key);
            slot++;
        }

        Map<String, String> mitigations = Map.of(
                "PacketRateLimiter", "packet-rate-limit.action",
                "InvalidPacket", "invalid-packet.action",
                "InvalidTeleport", "invalid-teleport.action",
                "InventoryDupeCheck", "inventory-exploit.action",
                "InvalidPlacementCheck", "invalid-placement.action",
                "ChunkCrashCheck", "chunk-crash.action"
        );
        for (var entry : mitigations.entrySet()) {
            var current = plugin.getConfigManager().getSettings().getMitigationMode(entry.getKey());
            inv.setItem(slot, labelled(Material.STONE, "§b" + entry.getKey() + " mitigation: " + current, List.of("Click to cycle")));
            mitigationSlots.put(slot, entry.getKey());
            slot++;
        }

        inv.setItem(52, labelled(Material.STONE, "Storage: " + plugin.getDatabaseManager().getPersistenceStatus(), List.of("Schema v" + plugin.getDatabaseManager().getSchemaVersion(), "Queued: " + plugin.getDatabaseManager().getPendingWriteCount())));
        inv.setItem(53, labelled(Material.STONE, "Integration: " + plugin.getIntegrationService().name(), List.of(
                "Webhook: " + plugin.getAlertManager().getLastWebhookStatus(),
                plugin.getAlertManager().isStructuredLoggingEnabled() ? "Structured log ✓" : "Structured log ✗")));

        viewerState.put(player.getUniqueId(), new GuiState(playerSlots, toggleSlots, mitigationSlots, page, state.highRiskOnly, state.lastActionAt));
        player.openInventory(inv);
        player.sendMessage("[ACAC] Click items to toggle mitigation or inspect players.");
    }

    public void openInspectGui(CommandSender sender, String target) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[UAC] Inspect GUI requires a player; showing text view instead.");
            checkManager.findPlayerId(target).ifPresent(uuid -> {
                CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(uuid);
                sender.sendMessage("[UAC] text inspect for " + target + " trust=" + stats.trustScore());
            });
            return;
        }
        var targetId = checkManager.findPlayerId(target);
        if (targetId.isEmpty()) {
            player.sendMessage("[ACAC] No recent data for " + target + ".");
            return;
        }
        inspectTargets.put(player.getUniqueId(), targetId.get());
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "ACAC Inspect: " + target);
        CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(targetId.get());
        inv.setItem(0, labelled(Material.STONE, "Trust: " + ONE_DECIMAL.format(stats.trustScore()), List.of(
                "Packets/s: " + ONE_DECIMAL.format(stats.packetsPerSecond()),
                "Flags: " + stats.flagCounts(),
                "Mitigation: " + stats.lastMitigation(),
                "Storage: " + plugin.getDatabaseManager().getPersistenceStatus())));
        inv.setItem(7, labelled(Material.DIRT, "Rubber-band", List.of("Send player to last safe position")));
        inv.setItem(8, labelled(Material.STONE, "Reset trust + flags", List.of("Clears counters for this player")));
        player.openInventory(inv);
    }

    /**
     * Handles inventory clicks. Returns true when the click is inside an ACAC GUI and should be cancelled.
     */
    public boolean handleClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (view == null || !(event.getWhoClicked() instanceof Player player)) {
            return false;
        }
        String title = view.getTitle();
        if (title == null || (!title.startsWith("ACAC Control") && !title.startsWith("ACAC Inspect"))) {
            return false;
        }
        event.setCancelled(true);
        if (title.startsWith("ACAC Control")) {
            GuiState state = viewerState.getOrDefault(player.getUniqueId(), new GuiState(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, false, 0));
            int slot = event.getSlot();
            int pages = Math.max(1, (int) Math.ceil(plugin.getServer().getOnlinePlayers().size() / (double) PLAYER_SLOTS));
            if (slot == 27 && state.page > 0) {
                viewerState.put(player.getUniqueId(), state.withPage(state.page - 1));
                openMainGui(player);
                return true;
            }
            if (slot == 28 && state.page < pages - 1) {
                viewerState.put(player.getUniqueId(), state.withPage(state.page + 1));
                openMainGui(player);
                return true;
            }
            if (slot == 29) {
                viewerState.put(player.getUniqueId(), state.withFilter(!state.highRiskOnly));
                openMainGui(player);
                return true;
            }
            if (slot == 30) {
                openMainGui(player);
                return true;
            }
            if (slot == 31) {
                if (!player.hasPermission("acac.gui.manage")) {
                    player.sendMessage("§cYou lack acac.gui.manage for global mitigation changes.");
                    return true;
                }
                plugin.getMitigationManager().setLogOnly(!plugin.getMitigationManager().isLogOnly());
                player.sendMessage("[ACAC] Global mitigation log-only=" + plugin.getMitigationManager().isLogOnly());
                openMainGui(player);
                return true;
            }
            if (state.toggleSlots.containsKey(slot)) {
                if (!player.hasPermission("acac.gui.manage") || !player.hasPermission("acac.admin")) {
                    player.sendMessage("§cYou need acac.admin and acac.gui.manage to toggle checks.");
                    return true;
                }
                String path = state.toggleSlots.get(slot);
                boolean next = toggleSetting(path);
                player.sendMessage("[ACAC] Toggled " + path + " to " + next);
                openMainGui(player);
                return true;
            }
            if (state.mitigationSlots.containsKey(slot)) {
                if (!player.hasPermission("acac.gui.manage") || !player.hasPermission("acac.admin")) {
                    player.sendMessage("§cYou need acac.admin and acac.gui.manage to change mitigations.");
                    return true;
                }
                String check = state.mitigationSlots.get(slot);
                var next = nextMitigation(check);
                player.sendMessage("[ACAC] " + check + " mitigation -> " + next);
                openMainGui(player);
                return true;
            }
            if (state.playerSlots.containsKey(slot)) {
                UUID targetId = state.playerSlots.get(slot);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.getUniqueId().equals(targetId))
                        .findFirst()
                        .ifPresent(p -> openInspectGui(player, p.getName()));
                return true;
            }
        }

        if (title.startsWith("ACAC Inspect")) {
            if (title.length() <= "ACAC Inspect: ".length()) {
                return true;
            }
            String targetName = title.substring("ACAC Inspect: ".length());
            UUID uuid = inspectTargets.get(player.getUniqueId());
            if (uuid == null) {
                return true;
            }
            GuiState state = viewerState.getOrDefault(player.getUniqueId(), new GuiState(new HashMap<>(), new HashMap<>(), new HashMap<>(), 0, false, 0));
            if (System.currentTimeMillis() - state.lastActionAt < ACTION_COOLDOWN_MS) {
                event.getWhoClicked().sendMessage("§cPlease wait before triggering another action.");
                return true;
            }
            if (event.getSlot() == 7) {
                if (!player.hasPermission("acac.admin")) {
                    player.sendMessage("§cYou need acac.admin to rubber-band players.");
                    return true;
                }
                Player targetPlayer = plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.getUniqueId().equals(uuid))
                        .findFirst().orElse(null);
                var stateData = checkManager.getOrCreateState(uuid);
                if (targetPlayer != null) {
                    plugin.getIntegrationService().getMitigationActions()
                            .rubberBand(targetPlayer, "GUI", stateData.getLastKnownPosition(), "Staff rubber-band");
                    viewerState.put(player.getUniqueId(), state.touch(System.currentTimeMillis()));
                } else {
                    player.sendMessage("§cPlayer is not online to rubber-band.");
                }
                return true;
            }
            if (event.getSlot() == 8) {
                if (!player.hasPermission("acac.admin")) {
                    player.sendMessage("§cYou need acac.admin to reset trust/flags.");
                    return true;
                }
                checkManager.resetTrust(uuid);
                checkManager.clearFlags(uuid);
                player.sendMessage("[ACAC] Cleared trust/flags for target.");
                viewerState.put(player.getUniqueId(), state.touch(System.currentTimeMillis()));
                openInspectGui(player, targetName);
            }
        }
        return true;
    }

    private boolean isEnabled(String key) {
        var s = plugin.getConfigManager().getSettings();
        return switch (key) {
            case "packet-rate-limit.enabled" -> s.packetRateLimiterEnabled;
            case "invalid-packet.enabled" -> s.invalidPacketEnabled;
            case "invalid-teleport.enabled" -> s.invalidTeleportEnabled;
            case "inventory-exploit.enabled" -> s.inventoryExploitEnabled;
            case "invalid-placement.enabled" -> s.invalidPlacementEnabled;
            case "chunk-crash.enabled" -> s.chunkCrashEnabled;
            case "entity-overload.enabled" -> s.entityOverloadEnabled;
            case "command-abuse.enabled" -> s.commandAbuseEnabled;
            case "sign-payload.enabled" -> s.signPayloadEnabled;
            case "redstone-exploit.enabled" -> s.redstoneEnabled;
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

    private ItemStack labelled(Material type, String name, List<String> lore) {
        ItemStack stack = new ItemStack(type);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('§', name));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
