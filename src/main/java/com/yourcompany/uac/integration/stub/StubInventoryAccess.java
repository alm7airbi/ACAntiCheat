package com.yourcompany.uac.integration.stub;

import com.yourcompany.uac.integration.bridge.InventoryAccess;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Stubbed inventory accessor that performs lightweight offline validation.
 */
public class StubInventoryAccess implements InventoryAccess {
    @Override
    public boolean isIllegalItem(ItemStack itemStack, int maxStackSize, int maxEnchantmentLevel) {
        if (itemStack == null) {
            return false;
        }
        if (itemStack.getAmount() > maxStackSize) {
            return true;
        }
        return itemStack.getEnchantments().values().stream().anyMatch(level -> level > maxEnchantmentLevel);
    }

    @Override
    public void rollbackContainerChange(Player player, String reason) {
        if (player != null) {
            player.sendMessage("[ACAC] (stub) would rollback container change: " + reason);
        }
    }
}
