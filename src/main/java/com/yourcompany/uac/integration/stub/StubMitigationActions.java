package com.yourcompany.uac.integration.stub;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.integration.bridge.MitigationActions;
import org.bukkit.entity.Player;

/**
 * Logs-only mitigation actions for offline builds.
 */
public class StubMitigationActions implements MitigationActions {

    private final UltimateAntiCheatPlugin plugin;

    public StubMitigationActions(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void warn(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] Would WARN " + player.getName() + " for " + checkName + ": " + reason);
    }

    @Override
    public void cancelAction(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] Would CANCEL action for " + player.getName() + " via " + checkName + ": " + reason);
    }

    @Override
    public void rollbackPlacement(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] Would ROLLBACK placement for " + player.getName() + " via " + checkName + ": " + reason);
    }

    @Override
    public void rollbackInventory(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] Would ROLLBACK inventory for " + player.getName() + " via " + checkName + ": " + reason);
    }

    @Override
    public void throttle(Player player, String checkName, String reason) {
        plugin.getLogger().info("[ACAC] Would THROTTLE " + player.getName() + " via " + checkName + ": " + reason);
    }

    @Override
    public void clearEntitiesNear(Player player, String checkName, int radius, String reason) {
        plugin.getLogger().info("[ACAC] Would CLEAR entities (r=" + radius + ") for " + player.getName() + " via " + checkName + ": " + reason);
    }

    @Override
    public void temporaryKick(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] Would TEMP KICK " + player.getName() + " for " + checkName + ": " + reason);
    }

    @Override
    public void temporaryBan(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] Would TEMP BAN " + player.getName() + " for " + checkName + ": " + reason);
    }

    @Override
    public void permanentBan(Player player, String checkName, String reason) {
        plugin.getLogger().warning("[ACAC] Would PERM BAN " + player.getName() + " for " + checkName + ": " + reason);
    }

    @Override
    public void rubberBand(Player player, String checkName, com.yourcompany.uac.checks.PlayerCheckState.Position lastPosition, String reason) {
        plugin.getLogger().info("[ACAC] Would RUBBER-BAND " + player.getName() + " for " + checkName + " to " + lastPosition + " because " + reason);
    }
}
