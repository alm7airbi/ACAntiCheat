package com.yourcompany.uac.integration.paper;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.integration.bridge.MitigationActions;
import org.bukkit.entity.Player;

/**
 * Placeholder for real Paper enforcement hooks. These log what would happen and
 * are intended to be filled with Bukkit kick/ban/cancel APIs on a live server.
 */
public class PaperMitigationActions implements MitigationActions {

    private final UltimateAntiCheatPlugin plugin;

    public PaperMitigationActions(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void warn(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) warn " + player.getName() + " via " + checkName + ": " + reason);
        player.sendMessage("§c[ACAC] Warning: " + reason);
    }

    @Override
    public void cancelAction(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) cancel action for " + player.getName() + " via " + checkName + ": " + reason);
        player.sendMessage("§e[ACAC] Action blocked: " + reason);
        // In Paper this is expected to be called from an event handler where event.setCancelled has already occurred.
    }

    @Override
    public void rollbackPlacement(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) rollback placement for " + player.getName() + " via " + checkName + ": " + reason);
        player.sendMessage("§c[ACAC] Placement reverted: " + reason);
        var world = player.getLocation() != null ? player.getLocation().getWorld() : null;
        if (world != null) {
            world.refreshChunk((int) player.getLocation().getX() >> 4, (int) player.getLocation().getZ() >> 4);
        }
    }

    @Override
    public void rollbackInventory(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) rollback inventory for " + player.getName() + " via " + checkName + ": " + reason);
        player.closeInventory();
        player.sendMessage("§c[ACAC] Inventory action reverted: " + reason);
    }

    @Override
    public void throttle(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) throttle actions for " + player.getName() + " via " + checkName + ": " + reason);
        player.sendMessage("§6[ACAC] You are being throttled: " + reason);
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
        plugin.getLogger().warning("[ACAC] (paper) temp kick " + player.getName() + " via " + checkName + ": " + reason);
        player.kickPlayer("ACAntiCheat: " + reason);
    }

    @Override
    public void temporaryBan(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] (paper) temp ban " + player.getName() + " via " + checkName + ": " + reason);
        var banList = plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME);
        banList.addBan(player.getName(), "ACAntiCheat (temporary): " + reason, java.util.Date.from(java.time.Instant.now().plus(java.time.Duration.ofMinutes(30))), "ACAC");
        player.kickPlayer("ACAntiCheat temp ban: " + reason);
    }

    @Override
    public void permanentBan(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] (paper) perm ban " + player.getName() + " via " + checkName + ": " + reason);
        var banList = plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME);
        banList.addBan(player.getName(), "ACAntiCheat: " + reason, null, "ACAC");
        player.kickPlayer("ACAntiCheat ban: " + reason);
    }

    @Override
    public void rubberBand(Player player, String checkName, com.yourcompany.uac.checks.PlayerCheckState.Position lastPosition, String reason) {
        plugin.getLogger().warning("[ACAC] (paper) rubber-banding " + player.getName() + " via " + checkName + " -> " + lastPosition + " reason=" + reason);
        org.bukkit.Location target = player.getLocation();
        if (lastPosition != null) {
            target = new org.bukkit.Location(player.getLocation().getWorld(), lastPosition.x(), lastPosition.y(), lastPosition.z());
        }
        player.teleportAsync(target);
        player.sendMessage("§c[ACAC] Movement corrected: " + reason);
    }
}
