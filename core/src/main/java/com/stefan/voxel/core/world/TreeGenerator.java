package com.stefan.voxel.core.world;

import org.joml.Vector3i;

/**
 * Generates tree structures in the world.
 *
 * Trees are placed as multi-block Structures so they can be removed as a whole.
 * Currently generates a simple oak-style tree: a trunk of LOG blocks topped with
 * a sphere-ish canopy of LEAVES blocks.
 *
 * Future: different tree types with different shapes/sizes, custom meshes, etc.
 */
public class TreeGenerator {

    private final PerlinNoise treeNoise;

    public TreeGenerator(long seed) {
        this.treeNoise = new PerlinNoise(seed + 100);
    }

    /**
     * Attempt to place trees across the generated terrain.
     * Uses noise to deterministically decide where trees go (seed-based).
     */
    public void generateTrees(World world, int startX, int startZ, int sizeX, int sizeZ) {
        // Use a coarse grid to space trees out, then noise to decide placement
        int spacing = 5; // Check every N blocks for a potential tree
        for (int x = startX; x < startX + sizeX; x += spacing) {
            for (int z = startZ; z < startZ + sizeZ; z += spacing) {
                double val = treeNoise.noise2D(x * 0.15, z * 0.15);
                if (val > 0.3) {
                    // Fine-tune position within the grid cell using noise
                    int offsetX = (int) (Math.abs(treeNoise.noise2D(x * 0.7, z * 0.3)) * (spacing - 1));
                    int offsetZ = (int) (Math.abs(treeNoise.noise2D(x * 0.3, z * 0.7)) * (spacing - 1));
                    int tx = x + offsetX;
                    int tz = z + offsetZ;

                    // Only place on grass
                    int surfaceY = world.getHighestBlock(tx, tz);
                    if (surfaceY < 1) continue;
                    if (world.getBlock(tx, surfaceY, tz) != BlockType.GRASS) continue;

                    // Vary trunk height with noise (4-7 blocks)
                    int trunkHeight = 4 + (int) (Math.abs(treeNoise.noise2D(tx * 0.5, tz * 0.5)) * 4);
                    if (trunkHeight > 7) trunkHeight = 7;

                    placeTree(world, tx, surfaceY + 1, tz, trunkHeight);
                }
            }
        }
    }

    /**
     * Place a single tree at the given base position and register it as a Structure.
     */
    private void placeTree(World world, int baseX, int baseY, int baseZ, int trunkHeight) {
        Structure tree = new Structure("tree", new Vector3i(baseX, baseY, baseZ));

        // Trunk
        for (int y = 0; y < trunkHeight; y++) {
            int bx = baseX, by = baseY + y, bz = baseZ;
            world.setBlock(bx, by, bz, BlockType.LOG);
            tree.addBlock(new Vector3i(bx, by, bz));
        }

        // Canopy (leaves) — a rough sphere around the top of the trunk
        int canopyBase = baseY + trunkHeight - 2;
        int canopyTop = baseY + trunkHeight + 1;
        int canopyRadius = 2;

        for (int y = canopyBase; y <= canopyTop; y++) {
            // Narrower at top and bottom
            int r = (y == canopyBase || y == canopyTop) ? canopyRadius - 1 : canopyRadius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Skip corners for rounder shape
                    if (Math.abs(dx) == r && Math.abs(dz) == r) continue;

                    int bx = baseX + dx, by = y, bz = baseZ + dz;
                    // Don't overwrite trunk
                    if (world.getBlock(bx, by, bz) == BlockType.LOG) continue;
                    // Don't overwrite existing solid blocks
                    if (world.getBlock(bx, by, bz) != BlockType.AIR) continue;

                    world.setBlock(bx, by, bz, BlockType.LEAVES);
                    tree.addBlock(new Vector3i(bx, by, bz));
                }
            }
        }

        world.addStructure(tree);
    }
}
