package com.stefan.voxel.core.world;

import org.joml.Vector3i;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final Map<Vector3i, Chunk> chunks = new HashMap<>();

    public void setBlock(int x, int y, int z, BlockType type) {
        Vector3i chunkPos = getChunkPosition(x, y, z);
        Chunk chunk = chunks.computeIfAbsent(chunkPos, Chunk::new);
        
        int lx = Math.floorMod(x, Chunk.SIZE);
        int ly = Math.floorMod(y, Chunk.SIZE);
        int lz = Math.floorMod(z, Chunk.SIZE);
        
        chunk.setBlock(lx, ly, lz, type);
    }

    public BlockType getBlock(int x, int y, int z) {
        Vector3i chunkPos = getChunkPosition(x, y, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return BlockType.AIR;

        int lx = Math.floorMod(x, Chunk.SIZE);
        int ly = Math.floorMod(y, Chunk.SIZE);
        int lz = Math.floorMod(z, Chunk.SIZE);

        return chunk.getBlock(lx, ly, lz);
    }

    private Vector3i getChunkPosition(int x, int y, int z) {
        return new Vector3i(
            Math.floorDiv(x, Chunk.SIZE),
            Math.floorDiv(y, Chunk.SIZE),
            Math.floorDiv(z, Chunk.SIZE)
        );
    }

    public Map<Vector3i, Chunk> getChunks() {
        return chunks;
    }
}
