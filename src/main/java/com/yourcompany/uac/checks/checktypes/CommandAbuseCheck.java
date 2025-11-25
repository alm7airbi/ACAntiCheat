package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.CommandContext;
import com.yourcompany.uac.config.Settings;

/**
 * Detects command spam that could lead to exploitable lag or crasher behaviour.
 */
public class CommandAbuseCheck extends AbstractCheck {

    public CommandAbuseCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "CommandAbuseCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof CommandContext command)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.commandAbuseEnabled) {
            return;
        }

        if (command.getWindowCount() > settings.maxCommandsPerWindow) {
            flag(command.getPlayer(), "Command spam: " + command.getWindowCount() + " in window", command.getCommandLine(), settings.commandAbuseSeverity);
            var actions = plugin.getIntegrationService().getMitigationActions();
            actions.throttle(command.getPlayer(), getCheckName(), "Command spam");
            actions.cancelAction(command.getPlayer(), getCheckName(), "Command spam");
        }
    }
}
