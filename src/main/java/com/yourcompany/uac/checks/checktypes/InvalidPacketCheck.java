package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.checks.context.MovementContext;

/**
 * Validates basic invariants on movement/position packets to avoid NaN/INF
 * coordinates and unrealistic displacements.
 */
public class InvalidPacketCheck extends AbstractCheck {

    public InvalidPacketCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "InvalidPacket");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof MovementContext move)) {
            return;
        }

        if (!plugin.getConfigManager().getSettings().invalidPacketEnabled) {
            return;
        }

        double x = move.getX();
        double y = move.getY();
        double z = move.getZ();
        int severity = plugin.getConfigManager().getSettings().invalidPacketSeverity;

        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            flag(move.getPlayer(), "Non-finite movement coordinates", move.getRawPacket(), severity + 1);
            plugin.getIntegrationService().getMitigationActions().cancelAction(move.getPlayer(), getCheckName(), "Non-finite coords");
            return;
        }

        double maxCoordinate = plugin.getConfigManager().getSettings().maxCoordinateMagnitude;
        if (Math.abs(x) > maxCoordinate || Math.abs(z) > maxCoordinate) {
            flag(move.getPlayer(), "Coordinate overflow (|x|/|z| > " + maxCoordinate + ")", move.getRawPacket(), severity + 1);
            plugin.getIntegrationService().getMitigationActions().cancelAction(move.getPlayer(), getCheckName(), "Coordinate overflow");
            return;
        }

        PlayerCheckState.Position last = move.getState().getLastKnownPosition();
        if (last != null) {
            double delta = Math.sqrt(PlayerCheckState.position(x, y, z).distanceSquared(last));
            double maxDelta = plugin.getConfigManager().getSettings().maxTeleportDelta;
            if (delta > maxDelta) {
                flag(move.getPlayer(), "Position jump " + delta + " exceeds max delta " + maxDelta, move.getRawPacket(), Math.max(1, severity));
                plugin.getIntegrationService().getMitigationActions().cancelAction(move.getPlayer(), getCheckName(), "Position snapback");
            }
        }
    }
}
