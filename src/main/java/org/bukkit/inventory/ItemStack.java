package org.bukkit.inventory;

public class ItemStack {
    private final Material type;
    private int amount = 1;
    private String displayName;
    private java.util.List<String> lore;

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

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setLore(java.util.List<String> lore) {
        this.lore = lore;
    }

    public java.util.List<String> getLore() {
        return lore;
    }

    public int getAppliedLevel() {
        return 0;
    }
}
