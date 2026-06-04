package com.stefan.voxel.core.world;

import org.joml.Vector3i;

import java.util.*;

/**
 * Represents a multi-block structure in the world (e.g., a tree, building, etc.).
 *
 * A structure is a collection of block positions that form a logical unit.
 * When any block belonging to a structure is destroyed, the entire structure
 * can be removed as a whole.
 *
 * This design is extensible:
 * - Future: structures can have durability, health, or other properties.
 * - Future: different structure types can override removal behavior (e.g., partial damage).
 * - Future: structures can have custom meshes instead of block-based rendering.
 */
public class Structure {
    private final String type;
    private final Vector3i origin;
    private final Set<Vector3i> blockPositions;

    public Structure(String type, Vector3i origin) {
        this.type = type;
        this.origin = new Vector3i(origin);
        this.blockPositions = new HashSet<>();
    }

    /**
     * Register a block position as part of this structure.
     */
    public void addBlock(Vector3i pos) {
        blockPositions.add(new Vector3i(pos));
    }

    public String getType() {
        return type;
    }

    public Vector3i getOrigin() {
        return origin;
    }

    public Set<Vector3i> getBlockPositions() {
        return Collections.unmodifiableSet(blockPositions);
    }
}
