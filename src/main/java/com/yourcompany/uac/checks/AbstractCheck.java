package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.entity.Player;

/**
 * Base class for all detection logic. Each check receives a context object
 * (packet payload, inventory event, etc.) and may flag/mitigate issues.
 */
public abstract class AbstractCheck {
    protected final UltimateAntiCheatPlugin plugin;
    protected final String checkName;
    private CheckManager checkManager;

    public AbstractCheck(UltimateAntiCheatPlugin plugin, String checkName) {
        this.plugin = plugin;
        this.checkName = checkName;
    }

    public abstract void handle(Object context);

    public void attachCheckManager(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    protected void flag(Player player, String reason, Object data) {
        flag(player, reason, data, 1);
    }

    protected void flag(Player player, String reason, Object data, int severity) {
        if (checkManager != null) {
            checkManager.recordFlag(player, checkName, reason, severity, data);
            return;
        }
        // TODO: integrate buffering/trust score mitigation and sanctioning when CheckManager is unavailable.
        plugin.getLogger().warning("[UAC] Check triggered: " + checkName + " Reason: " + reason + " Data: " + data);
    }

    public String getCheckName() {
        return checkName;
    }
}
