package com.stefan.voxel.core.storage;

import com.stefan.voxel.core.world.Chunk;
import com.stefan.voxel.core.world.World;
import org.joml.Vector3i;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * File-system based world storage.
 *
 * Directory layout:
 *   saves/
 *     worldName/
 *       world.dat    — metadata (seed, player position)
 *       chunks/
 *         x_y_z.chunk — raw chunk block data (byte[])
 */
public class FileWorldStorage implements WorldStorage {

    private final Path savesDir;

    public FileWorldStorage(Path savesDir) {
        this.savesDir = savesDir;
    }

    /**
     * Uses a default "saves" directory in the current working directory.
     */
    public FileWorldStorage() {
        this(Paths.get("saves"));
    }

    @Override
    public void save(WorldInfo info, World world) {
        try {
            Path worldDir = savesDir.resolve(info.getName());
            Path chunksDir = worldDir.resolve("chunks");
            Files.createDirectories(chunksDir);

            // Save metadata
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(worldDir.resolve("world.dat"))))) {
                dos.writeInt(1); // format version
                dos.writeLong(info.getSeed());
                dos.writeFloat(info.getPlayerX());
                dos.writeFloat(info.getPlayerY());
                dos.writeFloat(info.getPlayerZ());
            }

            // Save each chunk as raw bytes
            for (Map.Entry<Vector3i, Chunk> entry : world.getChunks().entrySet()) {
                Vector3i pos = entry.getKey();
                Chunk chunk = entry.getValue();
                String fileName = pos.x + "_" + pos.y + "_" + pos.z + ".chunk";
                Files.write(chunksDir.resolve(fileName), chunk.getBlocks());
            }

            System.out.println("World saved: " + info.getName());
        } catch (IOException e) {
            System.err.println("Failed to save world: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public World load(String worldName) {
        try {
            Path worldDir = savesDir.resolve(worldName);
            Path chunksDir = worldDir.resolve("chunks");
            if (!Files.exists(chunksDir)) return null;

            World world = new World();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunksDir, "*.chunk")) {
                for (Path chunkFile : stream) {
                    String name = chunkFile.getFileName().toString().replace(".chunk", "");
                    String[] parts = name.split("_");
                    if (parts.length != 3) continue;

                    int cx = Integer.parseInt(parts[0]);
                    int cy = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);

                    byte[] data = Files.readAllBytes(chunkFile);
                    Vector3i pos = new Vector3i(cx, cy, cz);
                    Chunk chunk = new Chunk(pos);

                    // Copy block data into the chunk
                    byte[] blocks = chunk.getBlocks();
                    System.arraycopy(data, 0, blocks, 0, Math.min(data.length, blocks.length));

                    world.getChunks().put(pos, chunk);
                }
            }

            System.out.println("World loaded: " + worldName + " (" + world.getChunks().size() + " chunks)");
            return world;
        } catch (IOException e) {
            System.err.println("Failed to load world: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public WorldInfo loadInfo(String worldName) {
        try {
            Path datFile = savesDir.resolve(worldName).resolve("world.dat");
            if (!Files.exists(datFile)) return null;

            try (DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(datFile)))) {
                int version = dis.readInt();
                long seed = dis.readLong();
                float px = dis.readFloat();
                float py = dis.readFloat();
                float pz = dis.readFloat();

                WorldInfo info = new WorldInfo(worldName, seed);
                info.setPlayerPosition(px, py, pz);
                return info;
            }
        } catch (IOException e) {
            System.err.println("Failed to load world info: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> listWorlds() {
        try {
            if (!Files.exists(savesDir)) return Collections.emptyList();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(savesDir)) {
                List<String> worlds = new ArrayList<>();
                for (Path entry : stream) {
                    if (Files.isDirectory(entry) && Files.exists(entry.resolve("world.dat"))) {
                        worlds.add(entry.getFileName().toString());
                    }
                }
                return worlds;
            }
        } catch (IOException e) {
            System.err.println("Failed to list worlds: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void delete(String worldName) {
        try {
            Path worldDir = savesDir.resolve(worldName);
            if (!Files.exists(worldDir)) return;

            // Delete chunks first
            Path chunksDir = worldDir.resolve("chunks");
            if (Files.exists(chunksDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunksDir)) {
                    for (Path file : stream) {
                        Files.delete(file);
                    }
                }
                Files.delete(chunksDir);
            }

            // Delete metadata
            Path datFile = worldDir.resolve("world.dat");
            if (Files.exists(datFile)) Files.delete(datFile);

            Files.delete(worldDir);
            System.out.println("World deleted: " + worldName);
        } catch (IOException e) {
            System.err.println("Failed to delete world: " + e.getMessage());
        }
    }
}
