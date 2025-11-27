package com.yourcompany.uac.integration.paper;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.integration.bridge.MitigationActions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

/**
 * Placeholder for real Paper enforcement hooks. These log what would happen and
 * are intended to be filled with Bukkit kick/ban/cancel APIs on a live server.
 */
public class PaperMitigationActions implements MitigationActions {

    private final UltimateAntiCheatPlugin plugin;
    private final com.yourcompany.uac.config.Settings settings;
    private final BukkitScheduler scheduler;
    private final ExternalPunishmentBridge punishmentBridge;

    public PaperMitigationActions(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.settings = plugin.getConfigManager().getSettings();
        this.scheduler = plugin.getServer().getScheduler();
        this.punishmentBridge = new ExternalPunishmentBridge(plugin, settings);
    }

    @Override
    public void warn(Player player, String checkName, String reason) {
        audit("warn", player, checkName, reason);
        player.sendMessage("§c[ACAC] Warning: " + reason);
    }

    @Override
    public void cancelAction(Player player, String checkName, String reason) {
        audit("cancel", player, checkName, reason);
        player.sendMessage("§e[ACAC] Action blocked: " + reason);
        runSync(() -> {
            player.closeInventory();
            player.setVelocity(new Vector());
        });
    }

    @Override
    public void rollbackPlacement(Player player, String checkName, String reason) {
        audit("rollback-placement", player, checkName, reason);
        player.sendMessage("§c[ACAC] Placement reverted: " + reason);
        var state = plugin.getCheckManager().getOrCreateState(player.getUniqueId());
        var lastPlacement = state.getLastPlacementPosition();
        if (lastPlacement == null || player.getLocation() == null || player.getLocation().getWorld() == null) {
            return;
        }
        runSync(() -> {
            var world = player.getLocation().getWorld();
            var chunk = world.getChunkAt((int) lastPlacement.x() >> 4, (int) lastPlacement.z() >> 4);
            if (!chunk.isLoaded() && settings.rollbackForceChunkLoad) {
                chunk.load();
            }
            var block = world.getBlockAt((int) lastPlacement.x(), (int) lastPlacement.y(), (int) lastPlacement.z());
            block.setType(Material.AIR, false);
            world.refreshChunk(chunk.getX(), chunk.getZ());
        });
    }

    @Override
    public void rollbackInventory(Player player, String checkName, String reason) {
        audit("rollback-inventory", player, checkName, reason);
        var state = plugin.getCheckManager().getOrCreateState(player.getUniqueId());
        var snapshot = state.getLastInventoryContents();
        long ageMillis = System.currentTimeMillis() - state.getLastInventorySnapshotAt();
        boolean snapshotValid = snapshot != null && ageMillis <= settings.inventorySnapshotMaxAgeSeconds * 1000L;
        runSync(() -> {
            player.closeInventory();
            if (snapshotValid) {
                player.getInventory().setContents(snapshot);
            }
            player.sendMessage("§c[ACAC] Inventory action reverted: " + reason);
            player.updateInventory();
        });
    }

    @Override
    public void throttle(Player player, String checkName, String reason) {
        audit("throttle", player, checkName, reason);
        player.sendMessage("§6[ACAC] You are being throttled: " + reason);
        var state = plugin.getCheckManager().getOrCreateState(player.getUniqueId());
        state.setUnderMitigation(true);
        long cooldown = Math.max(1500, settings.mitigationCooldownMillis);
        scheduler.runTaskLater(plugin, () -> {
            state.setUnderMitigation(false);
            state.setMitigationNote("Throttle expired", System.currentTimeMillis());
        }, cooldown / 50);
    }

    @Override
    public void clearEntitiesNear(Player player, String checkName, int radius, String reason) {
        plugin.getLogger().info("[ACAC] (paper) clear entities radius=" + radius + " near " + player.getName() + " via " + checkName + ": " + reason);
        if (player.getLocation() != null && player.getLocation().getWorld() != null) {
            var world = player.getLocation().getWorld();
            world.getNearbyEntities(player.getLocation(), radius, radius, radius).forEach(entity -> {
                if (!(entity instanceof Player)) {
                    entity.remove();
                }
            });
        }
    }

    @Override
    public void temporaryKick(Player player, String checkName, String reason) {
        audit("kick", player, checkName, reason);
        if (punishmentBridge.dispatchKick(player, checkName, reason)) {
            return;
        }
        runSync(() -> player.kickPlayer(settings.kickMessage + " (" + reason + ")"));
    }

    @Override
    public void temporaryBan(Player player, String checkName, String reason) {
        audit("temp-ban", player, checkName, reason);
        if (!punishmentBridge.dispatchTemporaryBan(player, checkName, reason, settings.temporaryBanMinutes)) {
            runSync(() -> {
                var banList = plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME);
                banList.addBan(player.getName(), settings.banMessage + " (temporary): " + reason,
                        java.util.Date.from(java.time.Instant.now().plus(java.time.Duration.ofMinutes(settings.temporaryBanMinutes))), "ACAC");
                player.kickPlayer(settings.banMessage + " (" + reason + ")");
            });
        }
    }

    @Override
    public void permanentBan(Player player, String checkName, String reason) {
        audit("perm-ban", player, checkName, reason);
        if (!punishmentBridge.dispatchPermanentBan(player, checkName, reason)) {
            runSync(() -> {
                var banList = plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME);
                banList.addBan(player.getName(), settings.banMessage + ": " + reason, null, "ACAC");
                player.kickPlayer(settings.banMessage + " (" + reason + ")");
            });
        }
    }

    @Override
    public void rubberBand(Player player, String checkName, com.yourcompany.uac.checks.PlayerCheckState.Position lastPosition, String reason) {
        audit("rubberband", player, checkName, reason);
        org.bukkit.Location target = player.getLocation();
        if (lastPosition != null) {
            target = new org.bukkit.Location(player.getLocation().getWorld(), lastPosition.x(), lastPosition.y(), lastPosition.z());
        }
        org.bukkit.Location finalTarget = target;
        runSync(() -> {
            player.teleport(finalTarget);
            player.setVelocity(new Vector());
            player.sendMessage("§c[ACAC] Movement corrected: " + reason);
        });
    }

    private void audit(String action, Player player, String checkName, String reason) {
        if (settings.auditMitigations) {
            plugin.getLogger().info("[ACAC] (paper) " + action + " for " + player.getName() + " via " + checkName + ": " + reason);
        }
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            scheduler.runTask(plugin, runnable);
        }
    }
}
