package com.yourcompany.uac.checks.checktypes.network;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.PacketContext;
import com.yourcompany.uac.integration.bridge.MitigationActions;

import java.nio.ByteBuffer;

/**
 * Detects obviously malicious payload sizes that attempt to overwhelm Netty/Paper decoders.
 */
public class NettyCrashProtectionCheck extends AbstractCheck {

    public NettyCrashProtectionCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "NettyCrashProtection");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PacketContext packet)) {
            return;
        }

        if (!plugin.getConfigManager().getSettings().enableNettyCrashProtection) {
            return;
        }

        Object raw = packet.getRawPacket();
        int length = estimateLength(raw);
        int maxBytes = plugin.getConfigManager().getSettings().nettyCrashMaxBytes;
        int severity = plugin.getConfigManager().getSettings().nettyCrashSeverity;

        if (length > maxBytes) {
            flag(packet.getPlayer(), "Oversized packet payload: " + length + " bytes > " + maxBytes, raw, severity);
            MitigationActions actions = plugin.getIntegrationService().getMitigationActions();
            if (plugin.getConfigManager().getSettings().nettyMitigateOversized) {
                actions.throttle(packet.getPlayer(), getCheckName(), "Oversized payload");
                actions.cancelAction(packet.getPlayer(), getCheckName(), "Oversized payload");
            }
            actions.temporaryKick(packet.getPlayer(), getCheckName(), "Oversized payload > " + maxBytes + " bytes");
        }
    }

    private int estimateLength(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof byte[] arr) {
            return arr.length;
        }
        if (raw instanceof ByteBuffer buffer) {
            return buffer.remaining();
        }
        if (raw instanceof CharSequence seq) {
            return seq.length();
        }
        // Unknown packet type, treat as small to avoid false positives in stub mode.
        return 0;
    }
}

