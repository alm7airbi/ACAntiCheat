package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.config.Settings;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

/**
 * Validates container clicks for impossible stack sizes or enchantment levels
 * using the active {@link com.yourcompany.uac.integration.bridge.InventoryAccess}.
 */
public class InvalidItemCheck extends AbstractCheck {

    public InvalidItemCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "InvalidItemCheck");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof InventoryClickEvent event)) {
            return;
        }
        Settings settings = plugin.getConfigManager().getSettings();
        if (!settings.enableInvalidItemCheck) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        boolean illegal = plugin.getIntegrationService().getInventoryAccess()
                .isIllegalItem(item, settings.maxConfiguredStackSize, settings.maxConfiguredEnchantLevel);

        int displayLength = item.getDisplayName() != null ? item.getDisplayName().length() : 0;
        int loreLength = 0;
        if (item.getLore() != null) {
            for (String line : item.getLore()) {
                loreLength += line.length();
            }
        }

        boolean oversizedMeta = displayLength > settings.maxDisplayNameLength || loreLength > settings.maxLoreLength;
        boolean oversizeStack = item.getAmount() > Math.max(item.getMaxStackSize(), settings.maxConfiguredStackSize);

        if (illegal || oversizedMeta || oversizeStack) {
            String reason = illegal ? "Illegal item stack detected: " + item.getType()
                    : oversizeStack ? "Stack exceeds limit (" + item.getAmount() + ")" : "Item meta too large";
            flag(event.getWhoClicked(), reason, item, settings.invalidItemSeverity + 1);
            plugin.getIntegrationService().getInventoryAccess()
                    .rollbackContainerChange((org.bukkit.entity.Player) event.getWhoClicked(), reason);
            plugin.getIntegrationService().getMitigationActions().rollbackInventory((Player) event.getWhoClicked(), getCheckName(), reason);
        }
    }
}
