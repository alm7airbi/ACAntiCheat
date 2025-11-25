package org.bukkit;

import org.bukkit.entity.Entity;
import org.bukkit.block.Block;
import org.bukkit.inventory.Material;
import org.bukkit.Chunk;

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

    public Chunk getChunkAt(int x, int z) {
        return new Chunk(x, z);
    }

    public List<Entity> getNearbyEntities(Location center, double dx, double dy, double dz) {
        return new ArrayList<>(entities);
    }

    public Block getBlockAt(int x, int y, int z) {
        return new Block(new Location(this, x, y, z), Material.STONE);
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }
}
