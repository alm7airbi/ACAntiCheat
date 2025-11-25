package com.yourcompany.uac.checks.checktypes.world;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.MovementContext;
import com.yourcompany.uac.config.Settings;

/**
 * Flags players forcing excessive chunk transitions that could trigger chunk load storms.
 */
public class ChunkCrashCheck extends AbstractCheck {

    public ChunkCrashCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "ChunkCrashCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof MovementContext move)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.chunkCrashEnabled) {
            return;
        }

        if (move.getChunkChanges() > settings.maxChunkChanges) {
            flag(move.getPlayer(), "Chunk hop spike: " + move.getChunkChanges() + " in " + move.getChunkWindowSeconds() + "s", move, settings.chunkCrashSeverity);
            var actions = plugin.getIntegrationService().getMitigationActions();
            actions.throttle(move.getPlayer(), getCheckName(), "Chunk hop spike");
            actions.rubberBand(move.getPlayer(), getCheckName(), move.getState().getLastKnownPosition(), "Chunk hop spike");
        }
    }
}
