package org.bukkit.block;

import org.bukkit.Location;
import org.bukkit.inventory.Material;

public class Block {
    private final Location location;
    private final Material type;

    public Block(Location location, Material type) {
        this.location = location;
        this.type = type;
    }

    public Location getLocation() {
        return location;
    }

    public Material getType() {
        return type;
    }

    public void setType(Material material) {
        // stub setter for offline builds
    }

    public void setType(Material material, boolean applyPhysics) {
        setType(material);
    }

    public int getX() {
        return (int) location.getX();
    }

    public int getY() {
        return (int) location.getY();
    }

    public int getZ() {
        return (int) location.getZ();
    }
}
