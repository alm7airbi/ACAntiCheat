package org.bukkit.entity;

import org.bukkit.Location;

/**
 * Simplified entity stub used only for offline builds.
 */
public class Entity {
    private Location location = new Location(0, 0, 0);
    private boolean removed;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void remove() {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }
}
