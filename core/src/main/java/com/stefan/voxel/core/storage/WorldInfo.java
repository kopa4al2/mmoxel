package com.stefan.voxel.core.storage;

/**
 * Metadata about a saved world. Stored alongside the world data.
 */
public class WorldInfo {
    private final String name;
    private final long seed;
    private float playerX, playerY, playerZ;

    public WorldInfo(String name, long seed) {
        this.name = name;
        this.seed = seed;
    }

    public String getName() { return name; }
    public long getSeed() { return seed; }

    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }
    public float getPlayerZ() { return playerZ; }

    public void setPlayerPosition(float x, float y, float z) {
        this.playerX = x;
        this.playerY = y;
        this.playerZ = z;
    }
}
