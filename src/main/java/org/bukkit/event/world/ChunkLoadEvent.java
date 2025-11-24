package org.bukkit.event.world;

import org.bukkit.entity.Player;

public class ChunkLoadEvent {
    private final int chunkX;
    private final int chunkZ;
    private final Player source;

    public ChunkLoadEvent(int chunkX, int chunkZ, Player source) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.source = source;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Player getSource() {
        return source;
    }
}
