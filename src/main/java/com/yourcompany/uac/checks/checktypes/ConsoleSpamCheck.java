package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.ConsoleMessageContext;

/**
 * Soft-detect console/log spam attributed to a player. The real implementation
 * would hook into a logger appender to attribute noise to the offending actor.
 */
public class ConsoleSpamCheck extends AbstractCheck {

    public ConsoleSpamCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "ConsoleSpam");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof ConsoleMessageContext consoleCtx)) {
            return;
        }

        if (!plugin.getConfigManager().getSettings().consoleSpamEnabled) {
            return;
        }

        int limit = plugin.getConfigManager().getSettings().consoleMessagesPerWindow;
        if (consoleCtx.getRecentCount() > limit) {
            flag(consoleCtx.getPlayer(), "Console spam: " + consoleCtx.getRecentCount() + " > " + limit,
                    consoleCtx.getMessage(), 2);
            consoleCtx.getState().setUnderMitigation(true);
            plugin.getIntegrationService().getMitigationActions().throttle(consoleCtx.getPlayer(), getCheckName(), "Console spam");
        }
    }
}
