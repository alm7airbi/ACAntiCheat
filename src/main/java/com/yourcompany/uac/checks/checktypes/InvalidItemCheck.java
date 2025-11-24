package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.config.Settings;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Placeholder validation that will later validate NBT, stack sizes, and
 * impossible enchantments. Currently only demonstrates hook shape.
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

        // TODO: NBT validation + mitigation
        if (item.getAmount() > item.getMaxStackSize()) {
            flag(event.getWhoClicked(), "Stack overflow for item: " + item.getType(), item,
                    settings.invalidItemSeverity);
        }
        if (item.getAmount() > settings.maxConfiguredStackSize) {
            flag(event.getWhoClicked(), "Stack exceeds configured max: " + item.getAmount(), item,
                    settings.invalidItemSeverity + 1);
        }
        // TODO: wire inventory interaction timestamp into PlayerCheckState when Bukkit events are available.
    }
}
