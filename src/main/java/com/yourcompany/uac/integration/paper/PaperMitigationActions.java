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
    }

    @Override
    public void rollbackPlacement(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) rollback placement for " + player.getName() + " via " + checkName + ": " + reason);
        // TODO: integrate with WorldEdit-like rollback or block state restoration on Paper.
    }

    @Override
    public void rollbackInventory(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) rollback inventory for " + player.getName() + " via " + checkName + ": " + reason);
        // TODO: revert container snapshots / cancel transaction on real server.
    }

    @Override
    public void throttle(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] (paper) throttle actions for " + player.getName() + " via " + checkName + ": " + reason);
        // TODO: apply per-player cooldowns or ProtocolLib packet throttling.
    }

    @Override
    public void clearEntitiesNear(Player player, String checkName, int radius, String reason) {
        plugin.getLogger().info("[ACAC] (paper) clear entities radius=" + radius + " near " + player.getName() + " via " + checkName + ": " + reason);
        // TODO: enumerate nearby entities and remove hostile ones on Paper.
    }

    @Override
    public void temporaryKick(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] (paper) temp kick " + player.getName() + " via " + checkName + ": " + reason);
        player.kickPlayer("ACAntiCheat: " + reason);
    }

    @Override
    public void temporaryBan(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] (paper) temp ban " + player.getName() + " via " + checkName + ": " + reason);
        // TODO: integrate with ban plugin / Paper ban API
    }

    @Override
    public void rubberBand(Player player, String checkName, com.yourcompany.uac.checks.PlayerCheckState.Position lastPosition, String reason) {
        plugin.getLogger().warning("[ACAC] (paper) rubber-banding " + player.getName() + " via " + checkName + " -> " + lastPosition + " reason=" + reason);
        if (lastPosition != null) {
            player.teleport(new org.bukkit.Location(player.getLocation().getWorld(), lastPosition.x(), lastPosition.y(), lastPosition.z()));
        } else {
            player.teleport(player.getLocation());
        }
        player.sendMessage("§c[ACAC] Movement corrected: " + reason);
    }
}
