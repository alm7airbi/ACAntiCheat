package com.yourcompany.uac.integration.paper;

import com.yourcompany.uac.integration.bridge.InventoryAccess;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Shell implementation that will later leverage Paper APIs for NBT/enchantment
 * validation and container rollback.
 */
public class PaperInventoryAccess implements InventoryAccess {
    @Override
    public boolean isIllegalItem(ItemStack itemStack, int maxStackSize, int maxEnchantmentLevel) {
        if (itemStack == null) {
            return false;
        }

        if (itemStack.getAmount() > Math.max(maxStackSize, itemStack.getMaxStackSize())) {
            return true;
        }

        // Inspect enchantment levels reflectively so this class compiles against the
        // lightweight stubs while enforcing limits when real Paper APIs are present.
        try {
            var getEnchantments = itemStack.getClass().getMethod("getEnchantments");
            Object result = getEnchantments.invoke(itemStack);
            if (result instanceof java.util.Map<?, ?> map) {
                for (Object value : map.values()) {
                    if (value instanceof Integer level && level > maxEnchantmentLevel) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            // Stubs do not expose enchantment metadata; fall through to meta lookup.
        }

        try {
            var getMeta = itemStack.getClass().getMethod("getItemMeta");
            Object meta = getMeta.invoke(itemStack);
            if (meta != null) {
                var getEnchants = meta.getClass().getMethod("getEnchants");
                Object result = getEnchants.invoke(meta);
                if (result instanceof java.util.Map<?, ?> map) {
                    for (Object value : map.values()) {
                        if (value instanceof Integer level && level > maxEnchantmentLevel) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Meta may be absent in stub mode; rely on stack size guard.
        }

        return false;
    }

    @Override
    public void rollbackContainerChange(Player player, String reason) {
        if (player != null) {
            player.closeInventory();
            player.updateInventory();
            player.sendMessage("§c[ACAC] Inventory change reverted: " + reason);
        }
    }
}
