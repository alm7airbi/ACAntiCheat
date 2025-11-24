package com.yourcompany.uac.checks.checktypes.inventory;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.context.InventoryActionContext;
import com.yourcompany.uac.config.Settings;

/**
 * Looks for suspicious inventory spam that often accompanies dupe glitches
 * (rapid slot swaps, stacked container interactions, impossible slot indices).
 */
public class InventoryDupeCheck extends AbstractCheck {

    public InventoryDupeCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "InventoryDupeCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof InventoryActionContext action)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.inventoryExploitEnabled) {
            return;
        }

        int count = action.getWindowCount();
        if (count > settings.inventoryActionsPerWindow) {
            flag(action.getPlayer(), "Inventory spam: " + count + " actions in window", action.getActionType(),
                    settings.inventoryExploitSeverity);
        }

        // Impossible slots or negative slot ids can indicate packet tampering.
        if (action.getSlot() < 0 || action.getSlot() > settings.maxInventorySlotIndex) {
            flag(action.getPlayer(), "Invalid slot index " + action.getSlot(), action.getItem(),
                    settings.inventoryExploitSeverity + 1);
        }

        // TODO: when using Paper events, inspect click type + container source for illegal move patterns.
    }
}
