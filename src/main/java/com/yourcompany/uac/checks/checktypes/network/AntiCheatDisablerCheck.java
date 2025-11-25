package com.yourcompany.uac.checks.checktypes.network;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.PacketContext;
import com.yourcompany.uac.config.Settings;

/**
 * Watches for suspicious silence after periods of high activity which can
 * indicate client-sided anti-cheat disablers or packet filters.
 */
public class AntiCheatDisablerCheck extends AbstractCheck {

    public AntiCheatDisablerCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "AntiCheatDisablerCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PacketContext packetContext)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.disablerEnabled) {
            return;
        }

        int second = packetContext.getPacketsLastSecond();
        int fiveSeconds = packetContext.getPacketsLastFiveSeconds();
        // If the player had a burst and then suddenly goes silent, flag.
        if (fiveSeconds > settings.disablerHighWaterMark && second < settings.disablerSilenceThreshold) {
            flag(packetContext.getPlayer(), "Packet silence after burst (" + fiveSeconds + " -> " + second + ")", null,
                    settings.disablerSeverity);
            plugin.getIntegrationService().getMitigationActions().throttle(packetContext.getPlayer(), getCheckName(), "Potential disabler silence");
        }

    }
}
