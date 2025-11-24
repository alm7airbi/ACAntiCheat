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
}
