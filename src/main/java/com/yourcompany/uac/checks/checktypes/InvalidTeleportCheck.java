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
        double maxDistance = plugin.getConfigManager().getSettings().invalidTeleportMaxDistance;
        boolean serverTeleport = movement.isServerTeleport() || state.wasTeleportServerInitiated();

        if (!serverTeleport && distanceSquared > maxDistance * maxDistance) {
            flag(movement.getPlayer(), "Impossible teleport jump (" + Math.sqrt(distanceSquared) + " blocks)", movement.getRawPacket(), 3);
            state.setUnderMitigation(true);
            // TODO: teleport the player back on real Paper via Player#teleport.
        }

        state.recordMovement(PlayerCheckState.position(movement.getX(), movement.getY(), movement.getZ()), movement.getTimestamp(), movement.isServerTeleport());
    }
}
