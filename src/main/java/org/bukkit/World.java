package org.bukkit;

import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal world stub for offline builds.
 */
public class World {
    private final List<Entity> entities = new ArrayList<>();

    public void refreshChunk(int x, int z) {
        // no-op stub
    }

    public List<Entity> getNearbyEntities(Location center, double dx, double dy, double dz) {
        return new ArrayList<>(entities);
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }
}
