package com.yourcompany.uac.checks.checktypes.world;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.PlacementContext;
import com.yourcompany.uac.config.Settings;

/**
 * Detects impossible or abusive block placements such as extreme y-levels,
 * rapid placement spam, or compact machine building.
 */
public class InvalidPlacementCheck extends AbstractCheck {

    public InvalidPlacementCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "InvalidPlacementCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PlacementContext placement)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.invalidPlacementEnabled) {
            return;
        }

        if (placement.getPosition() != null && placement.getPosition().y() > settings.maxBuildHeight) {
            flag(placement.getPlayer(), "Placement above build height: " + placement.getPosition().y(), placement.getMaterial(),
                    settings.invalidPlacementSeverity + 1);
            plugin.getIntegrationService().getMitigationActions().rollbackPlacement(placement.getPlayer(), getCheckName(), "Above build height");
        }

        int windowCount = placement.getWindowCount();
        if (windowCount > settings.placementActionsPerWindow) {
            flag(placement.getPlayer(), "Block placement spam (" + windowCount + "/" + settings.placementWindowSeconds + "s)",
                    placement.getMaterial(), settings.invalidPlacementSeverity);
            plugin.getIntegrationService().getMitigationActions().throttle(placement.getPlayer(), getCheckName(), "Placement spam");
        }
    }
}
