package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
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
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        // TODO: NBT validation + mitigation
        if (item.getAmount() > item.getMaxStackSize()) {
            flag("Stack overflow for item: " + item.getType(), event.getWhoClicked().getName());
        }
    }
}
