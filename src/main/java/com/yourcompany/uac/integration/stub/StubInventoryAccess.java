package com.yourcompany.uac.integration.stub;

import com.yourcompany.uac.integration.bridge.InventoryAccess;
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
        return itemStack.getAmount() > maxStackSize || itemStack.getAppliedLevel() > maxEnchantmentLevel;
    }

    @Override
    public void rollbackContainerChange(Player player, String reason) {
        // TODO: replace with real Paper inventory rollback APIs when available.
        if (player != null) {
            player.sendMessage("[ACAC] (stub) would rollback container change: " + reason);
        }
    }
}
