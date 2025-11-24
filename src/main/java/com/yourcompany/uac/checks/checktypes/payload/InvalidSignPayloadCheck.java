package com.yourcompany.uac.checks.checktypes.payload;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.PayloadContext;
import com.yourcompany.uac.config.Settings;

/**
 * Guards against oversized sign edits or malicious plugin payloads that could
 * crash the server or bypass content restrictions.
 */
public class InvalidSignPayloadCheck extends AbstractCheck {

    public InvalidSignPayloadCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "InvalidSignPayloadCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PayloadContext payload)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.signPayloadEnabled) {
            return;
        }

        if (payload.getBytes() > settings.maxPayloadBytes) {
            flag(payload.getPlayer(), "Payload too large: " + payload.getBytes() + "B", payload.getChannel(),
                    settings.signPayloadSeverity + 1);
        }

        if (payload.getPayloadPreview() != null && payload.getPayloadPreview().length() > settings.maxSignCharacters) {
            flag(payload.getPlayer(), "Oversized sign text length=" + payload.getPayloadPreview().length(),
                    payload.getPayloadPreview(), settings.signPayloadSeverity);
        }

        // TODO: validate lines and strip unsafe characters with Paper sign change events.
    }
}
