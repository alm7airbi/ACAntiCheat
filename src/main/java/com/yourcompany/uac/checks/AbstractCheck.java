package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;

/**
 * Base class for all detection logic. Each check receives a context object
 * (packet payload, inventory event, etc.) and may flag/mitigate issues.
 */
public abstract class AbstractCheck {
    protected final UltimateAntiCheatPlugin plugin;
    protected final String checkName;

    public AbstractCheck(UltimateAntiCheatPlugin plugin, String checkName) {
        this.plugin = plugin;
        this.checkName = checkName;
    }

    public abstract void handle(Object context);

    protected void flag(String reason, Object data) {
        plugin.getLogger().warning("[UAC] Check triggered: " + checkName + " Reason: " + reason + " Data: " + data);
        // TODO: integrate buffering/trust score mitigation and sanctioning
    }

    public String getCheckName() {
        return checkName;
    }
}
