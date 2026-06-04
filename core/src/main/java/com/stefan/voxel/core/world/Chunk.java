package com.stefan.voxel.core.world;

import org.joml.Vector3i;

public class Chunk {
    public static final int SIZE = 16;
    private final Vector3i position;
    private final byte[] blocks;

    public Chunk(Vector3i position) {
        this.position = new Vector3i(position);
        this.blocks = new byte[SIZE * SIZE * SIZE];
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        blocks[getIndex(x, y, z)] = (byte) type.getId();
    }

    public BlockType getBlock(int x, int y, int z) {
        return BlockType.fromId(blocks[getIndex(x, y, z)]);
    }

    private int getIndex(int x, int y, int z) {
        return x + (y * SIZE) + (z * SIZE * SIZE);
    }

    public Vector3i getPosition() {
        return position;
    }

    public byte[] getBlocks() {
        return blocks;
    }
}
