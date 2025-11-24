package org.bukkit.inventory;

public class ItemStack {
    private final Material type;
    private int amount = 1;

    public ItemStack(Material type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getMaxStackSize() {
        return 64;
    }

    public Material getType() {
        return type;
    }
}
