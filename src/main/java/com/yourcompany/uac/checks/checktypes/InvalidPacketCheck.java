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
        var settings = plugin.getConfigManager().getSettings();

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
            double lagFactor = move.getEnvironment().lagFactor(settings.invalidPacketLagTpsFloor, settings.invalidPacketLagPingThreshold);
            double protocolScale = move.getEnvironment().protocolVersion() > 0 && move.getEnvironment().protocolVersion() < 340 ? 0.8 : 1.0;
            double maxDelta = settings.maxTeleportDelta * Math.max(1.0, lagFactor * 0.5) * protocolScale;
            if (delta > maxDelta) {
                flag(move.getPlayer(), "Position jump " + delta + " exceeds max delta " + maxDelta + " (ping=" + move.getEnvironment().ping() + " tps=" + move.getEnvironment().serverTps() + ")", move.getRawPacket(), Math.max(1, severity));
                plugin.getIntegrationService().getMitigationActions().cancelAction(move.getPlayer(), getCheckName(), "Position snapback");
            }

            double elapsedSeconds = Math.max(0.05, (move.getTimestamp() - move.getState().getLastMovementMillis()) / 1000.0);
            double dx = x - last.x();
            double dz = z - last.z();
            double dy = y - last.y();
            double horizontalSpeed = Math.sqrt(dx * dx + dz * dz) / elapsedSeconds;
            double verticalSpeed = Math.abs(dy) / elapsedSeconds;
            double allowedHorizontal = settings.maxHorizontalSpeed * Math.max(1.0, lagFactor);
            double allowedVertical = settings.maxVerticalSpeed * Math.max(1.0, lagFactor);
            if (horizontalSpeed > allowedHorizontal && !move.isServerTeleport()) {
                flag(move.getPlayer(), "Horizontal speed " + horizontalSpeed + " exceeds " + allowedHorizontal, move.getRawPacket(), severity + 1);
                plugin.getIntegrationService().getMitigationActions().rubberBand(move.getPlayer(), getCheckName(), move.getState().getLastKnownPosition(), "Horizontal speed");
            }
            if (verticalSpeed > allowedVertical && !move.isServerTeleport()) {
                flag(move.getPlayer(), "Vertical speed " + verticalSpeed + " exceeds " + allowedVertical, move.getRawPacket(), severity + 1);
                plugin.getIntegrationService().getMitigationActions().rubberBand(move.getPlayer(), getCheckName(), move.getState().getLastKnownPosition(), "Vertical speed");
            }
        }
    }
}
