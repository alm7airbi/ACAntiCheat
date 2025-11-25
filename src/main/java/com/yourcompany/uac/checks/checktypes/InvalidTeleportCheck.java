package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.checks.context.MovementContext;

/**
 * Detects impossible teleport spikes that are not server initiated.
 */
public class InvalidTeleportCheck extends AbstractCheck {

    public InvalidTeleportCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "InvalidTeleport");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof MovementContext movement)) {
            return;
        }

        if (!plugin.getConfigManager().getSettings().invalidTeleportEnabled) {
            return;
        }

        PlayerCheckState state = movement.getState();
        PlayerCheckState.Position last = state.getLastKnownPosition();
        if (last == null) {
            state.recordMovement(PlayerCheckState.position(movement.getX(), movement.getY(), movement.getZ()), movement.getTimestamp(), movement.isServerTeleport());
            return;
        }

        double distanceSquared = PlayerCheckState.position(movement.getX(), movement.getY(), movement.getZ()).distanceSquared(last);
        var settings = plugin.getConfigManager().getSettings();
        double maxDistance = settings.invalidTeleportMaxDistance;
        boolean serverTeleport = movement.isServerTeleport() || state.wasTeleportServerInitiated();
        int severity = plugin.getConfigManager().getSettings().invalidTeleportSeverity;
        long sinceTeleport = movement.getTimestamp() - state.getLastTeleportMillis();
        double lagBuffer = settings.invalidTeleportLagBuffer + Math.max(0, movement.getEnvironment().ping() - settings.invalidPacketLagPingThreshold) * 0.05;

        if (!serverTeleport && sinceTeleport > settings.teleportExemptMillis && distanceSquared > (maxDistance + lagBuffer) * (maxDistance + lagBuffer)) {
            flag(movement.getPlayer(), "Impossible teleport jump (" + Math.sqrt(distanceSquared) + " blocks)", movement.getRawPacket(), severity + 1);
            state.setUnderMitigation(true);
            state.setMitigationNote("Rubber-banded for invalid teleport", movement.getTimestamp());
            plugin.getIntegrationService().getMitigationActions().rubberBand(movement.getPlayer(), getCheckName(), state.getLastKnownPosition(), "Invalid teleport");
            plugin.getIntegrationService().getMitigationActions().cancelAction(movement.getPlayer(), getCheckName(), "Invalid teleport");
        }

        state.recordMovement(PlayerCheckState.position(movement.getX(), movement.getY(), movement.getZ()), movement.getTimestamp(), movement.isServerTeleport());
    }
}
