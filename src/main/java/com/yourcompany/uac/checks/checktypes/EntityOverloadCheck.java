package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.EntityActionContext;

/**
 * Flags rapid entity spawning/interaction to guard against crash attempts.
 */
public class EntityOverloadCheck extends AbstractCheck {

    public EntityOverloadCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "EntityOverload");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof EntityActionContext entityCtx)) {
            return;
        }

        if (!plugin.getConfigManager().getSettings().entityOverloadEnabled) {
            return;
        }

        int limit = plugin.getConfigManager().getSettings().entityActionsPerWindow;
        if (entityCtx.getRecentCount() > limit) {
            flag(entityCtx.getPlayer(), "Entity spam: " + entityCtx.getRecentCount() + " > " + limit,
                    entityCtx.getActionType(), 2);
            plugin.getIntegrationService().getMitigationActions().clearEntitiesNear(entityCtx.getPlayer(), getCheckName(), 8, "Entity overload");
        }
    }
}
