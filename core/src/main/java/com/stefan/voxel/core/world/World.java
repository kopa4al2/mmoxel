package com.stefan.voxel.core.world;

import org.joml.Vector3i;
import java.util.*;

public class World {
    private final Map<Vector3i, Chunk> chunks = new HashMap<>();
    private final List<Structure> structures = new ArrayList<>();
    private final Map<Vector3i, Structure> blockToStructure = new HashMap<>();

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

    /**
     * Find the highest non-air block at the given (x, z) column.
     * Scans from top down. Returns -1 if no solid block found.
     */
    public int getHighestBlock(int x, int z) {
        // Scan chunk columns from top to bottom
        int maxChunkY = Integer.MIN_VALUE;
        for (Vector3i pos : chunks.keySet()) {
            if (pos.x == Math.floorDiv(x, Chunk.SIZE) && pos.z == Math.floorDiv(z, Chunk.SIZE)) {
                maxChunkY = Math.max(maxChunkY, pos.y);
            }
        }
        if (maxChunkY == Integer.MIN_VALUE) return -1;

        for (int y = (maxChunkY + 1) * Chunk.SIZE - 1; y >= 0; y--) {
            if (getBlock(x, y, z) != BlockType.AIR) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Register a structure in the world and index all its blocks for fast lookup.
     */
    public void addStructure(Structure structure) {
        structures.add(structure);
        for (Vector3i pos : structure.getBlockPositions()) {
            blockToStructure.put(new Vector3i(pos), structure);
        }
    }

    /**
     * Get the structure that owns the block at the given position, or null.
     */
    public Structure getStructureAt(int x, int y, int z) {
        return blockToStructure.get(new Vector3i(x, y, z));
    }

    /**
     * Remove a structure from the world: sets all its blocks to AIR and unregisters it.
     */
    public void removeStructure(Structure structure) {
        for (Vector3i pos : structure.getBlockPositions()) {
            setBlock(pos.x, pos.y, pos.z, BlockType.AIR);
            blockToStructure.remove(pos);
        }
        structures.remove(structure);
    }

    public Map<Vector3i, Chunk> getChunks() {
        return chunks;
    }
}
