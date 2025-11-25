package org.bukkit;

/**
 * Minimal chunk stub for offline builds.
 */
public class Chunk {
    private final int x;
    private final int z;
    private boolean loaded = true;

    public Chunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean load() {
        loaded = true;
        return true;
    }
}
