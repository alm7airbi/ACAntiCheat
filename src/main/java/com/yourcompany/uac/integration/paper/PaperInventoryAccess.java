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
        // TODO: inspect NBT/enchantments via ItemMeta when Paper is present.
        if (itemStack == null) {
            return false;
        }
        return itemStack.getAmount() > maxStackSize || itemStack.getAppliedLevel() > maxEnchantmentLevel;
    }

    @Override
    public void rollbackContainerChange(Player player, String reason) {
        // TODO: cancel inventory events or revert snapshots on real server.
        if (player != null) {
            player.sendMessage("[ACAC] (paper) would rollback container change: " + reason);
        }
    }
}
