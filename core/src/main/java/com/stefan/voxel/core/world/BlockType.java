package com.stefan.voxel.core.world;

public enum BlockType {
    AIR(0),
    DIRT(1),
    GRASS(2),
    STONE(3),
    SAND(4),
    WATER(5),
    BEDROCK(6),
    SNOW(7),
    GRAVEL(8),
    LOG(9),
    LEAVES(10);

    private final int id;

    BlockType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) return type;
        }
        return AIR;
    }
}
