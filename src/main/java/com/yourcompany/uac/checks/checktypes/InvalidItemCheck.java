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
        if (illegal) {
            flag(event.getWhoClicked(), "Illegal item stack detected: " + item.getType(), item,
                    settings.invalidItemSeverity + 1);
            plugin.getIntegrationService().getInventoryAccess()
                    .rollbackContainerChange((org.bukkit.entity.Player) event.getWhoClicked(), "Illegal item stack");
            plugin.getIntegrationService().getMitigationActions().rollbackInventory((Player) event.getWhoClicked(), getCheckName(), "Illegal item stack");
        }
    }
}
