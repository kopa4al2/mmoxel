package com.stefan.voxel.core.storage;

import com.stefan.voxel.core.world.World;

import java.util.List;

/**
 * Abstraction for world persistence. Implementations can store to filesystem,
 * database, or a remote server.
 */
public interface WorldStorage {

    /**
     * Save the world and its metadata.
     */
    void save(WorldInfo info, World world);

    /**
     * Load a world by name. Returns null if not found.
     */
    World load(String worldName);

    /**
     * Load the metadata for a world by name. Returns null if not found.
     */
    WorldInfo loadInfo(String worldName);

    /**
     * List all available saved worlds.
     */
    List<String> listWorlds();

    /**
     * Delete a saved world.
     */
    void delete(String worldName);
}
