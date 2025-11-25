package com.yourcompany.uac.integration.bridge;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Abstraction layer for inspecting or rolling back inventory/container changes.
 */
public interface InventoryAccess {
    boolean isIllegalItem(ItemStack itemStack, int maxStackSize, int maxEnchantmentLevel);

    void rollbackContainerChange(Player player, String reason);
}
