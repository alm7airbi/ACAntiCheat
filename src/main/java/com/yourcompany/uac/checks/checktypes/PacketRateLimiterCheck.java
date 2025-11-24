package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.checks.context.PacketContext;

/**
 * Tracks per-player incoming packet counts to detect flood/DoS attempts.
 */
public class PacketRateLimiterCheck extends AbstractCheck {

    public PacketRateLimiterCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "PacketRateLimiter");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PacketContext packet)) {
            return;
        }

        if (!plugin.getConfigManager().getSettings().packetRateLimiterEnabled) {
            return;
        }

        int perSecond = packet.getPacketsLastSecond();
        int perFiveSeconds = packet.getPacketsLastFiveSeconds();
        int perSecondLimit = plugin.getConfigManager().getSettings().packetRateLimitPerSecond;
        int perFiveSecondLimit = plugin.getConfigManager().getSettings().packetRateLimitPerFiveSeconds;
        int kickThreshold = plugin.getConfigManager().getSettings().packetRateKickThreshold;
        int severity = plugin.getConfigManager().getSettings().packetRateSeverity;

        PlayerCheckState state = packet.getState();
        if (perSecond > perSecondLimit) {
            flag(packet.getPlayer(), "Exceeded packet rate limit: " + perSecond + "/s > " + perSecondLimit, packet.getRawPacket(), severity);
            state.setMitigationNote("Rate limited for packet spam", packet.getTimestamp());
        }

        if (perFiveSeconds > perFiveSecondLimit) {
            flag(packet.getPlayer(), "Sustained packet load: " + perFiveSeconds + " in 5s", packet.getRawPacket(), Math.max(1, severity - 1));
        }

        if (perSecond > kickThreshold) {
            state.setUnderMitigation(true);
            state.setMitigationNote("Kick-queued for packet spam", packet.getTimestamp());
            flag(packet.getPlayer(), "Kick-queued for extreme packet spam (" + perSecond + "/s)", packet.getRawPacket(), severity + 1);
            plugin.getIntegrationService().getMitigationActions()
                    .throttle(packet.getPlayer(), getCheckName(), "Extreme packet spam");
            plugin.getIntegrationService().getMitigationActions()
                    .temporaryKick(packet.getPlayer(), getCheckName(), "Packet spam > " + kickThreshold + "/s");
        }
    }
}
