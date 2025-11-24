package com.yourcompany.uac.integration.bridge;

import org.bukkit.entity.Player;

/**
 * Bridge that will call real Paper/ProtocolLib APIs when available, or log-only
 * in this offline environment.
 */
public interface MitigationActions {
    void warn(Player player, String checkName, String reason);

    void cancelAction(Player player, String checkName, String reason);

    void rollbackPlacement(Player player, String checkName, String reason);

    void rollbackInventory(Player player, String checkName, String reason);

    void throttle(Player player, String checkName, String reason);

    void clearEntitiesNear(Player player, String checkName, int radius, String reason);

    void temporaryKick(Player player, String checkName, String reason);

    void temporaryBan(Player player, String checkName, String reason);

    void rubberBand(Player player, String checkName, com.yourcompany.uac.checks.PlayerCheckState.Position lastPosition, String reason);
}
