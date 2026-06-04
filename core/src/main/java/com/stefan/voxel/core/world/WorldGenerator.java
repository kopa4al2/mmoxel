package com.stefan.voxel.core.world;

/**
 * Seed-based procedural terrain generator.
 *
 * How terrain generation works:
 *
 * 1. HEIGHT MAP: For each (x, z) column, we sample Perlin noise to get a height value.
 *    The noise is scaled so terrain ranges from ~5 to ~45 blocks high, with dramatic hills and valleys.
 *
 * 2. LAYER STACKING: Once we know the height at (x, z), we fill blocks from bottom to top:
 *    - y=0: BEDROCK (unbreakable floor)
 *    - y=1 to height-4: STONE (deep underground)
 *    - y=height-3 to height-1: DIRT (just below surface)
 *    - y=height: Surface block (GRASS, SAND, or SNOW depending on conditions)
 *
 * 3. BIOME-LIKE VARIATION: A second, slower noise function determines "temperature" at each (x,z).
 *    - Cold areas (high altitude or cold noise): SNOW on top
 *    - Low areas near "sea level": SAND on top (like beaches)
 *    - Everything else: GRASS on top
 *
 * 4. CAVES: 3D Perlin noise carves out caves underground. If the 3D noise value at a block
 *    exceeds a threshold, that block becomes AIR — creating tunnels and caverns.
 *
 * 5. WATER: Any AIR block at or below sea level (WATER_LEVEL) gets filled with WATER,
 *    creating lakes and oceans in low-lying areas.
 *
 * 6. GRAVEL PATCHES: Another noise layer places gravel deposits underground for variety.
 *
 * Because everything is derived from the seed via Perlin noise, the same seed ALWAYS
 * produces the exact same world — you can share seeds with friends to play the same map.
 */
public class WorldGenerator {
    private final long seed;
    private final PerlinNoise heightNoise;
    private final PerlinNoise caveNoise;
    private final PerlinNoise biomeNoise;
    private final PerlinNoise detailNoise;
    private final TreeGenerator treeGenerator;

    // Terrain parameters
    private static final int BASE_HEIGHT = 20;        // Average terrain height
    private static final int HEIGHT_VARIATION = 20;    // Max height deviation — creates tall peaks and deep valleys
    private static final int WATER_LEVEL = 14;         // Sea level — below this, water fills gaps
    private static final double CAVE_THRESHOLD = 0.38; // Lower = more caves (was 0.55, now much more aggressive)
    private static final double HEIGHT_SCALE = 0.015;  // Lower = bigger, more dramatic hills
    private static final double CAVE_SCALE = 0.06;     // Controls cave tunnel size
    private static final double BIOME_SCALE = 0.008;   // Slow variation for biome-like regions

    public WorldGenerator(long seed) {
        this.seed = seed;
        // Each noise generator uses a different seed offset so they produce independent patterns
        this.heightNoise = new PerlinNoise(seed);
        this.caveNoise = new PerlinNoise(seed + 1);
        this.biomeNoise = new PerlinNoise(seed + 2);
        this.detailNoise = new PerlinNoise(seed + 3);
        this.treeGenerator = new TreeGenerator(seed);
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Generate terrain for a rectangular area and populate the given World.
     */
    public void generate(World world, int startX, int startZ, int sizeX, int sizeZ) {
        // First pass: generate terrain
        for (int x = startX; x < startX + sizeX; x++) {
            for (int z = startZ; z < startZ + sizeZ; z++) {
                generateColumn(world, x, z);
            }
        }
        // Second pass: place trees on top of the generated terrain
        treeGenerator.generateTrees(world, startX, startZ, sizeX, sizeZ);
    }

    /**
     * Generate a single vertical column of blocks at (x, z).
     */
    private void generateColumn(World world, int x, int z) {
        // Step 1: Calculate terrain height using layered Perlin noise
        double heightValue = heightNoise.octaveNoise2D(x * HEIGHT_SCALE, z * HEIGHT_SCALE, 5, 0.5);
        // Add finer detail for rougher terrain
        double detail = detailNoise.octaveNoise2D(x * HEIGHT_SCALE * 4, z * HEIGHT_SCALE * 4, 3, 0.5);
        int terrainHeight = BASE_HEIGHT + (int) (heightValue * HEIGHT_VARIATION + detail * 5);
        // Clamp to reasonable range
        if (terrainHeight < 2) terrainHeight = 2;

        // Step 2: Determine surface type based on biome noise and height
        double biomeValue = biomeNoise.octaveNoise2D(x * BIOME_SCALE, z * BIOME_SCALE, 3, 0.5);
        BlockType surfaceBlock = determineSurfaceBlock(terrainHeight, biomeValue);

        // Step 3: Fill the column from bottom to top
        // Bedrock at the very bottom
        world.setBlock(x, 0, z, BlockType.BEDROCK);

        for (int y = 1; y <= terrainHeight; y++) {
            // Check for caves (don't carve through bedrock or the very surface)
            if (y > 1 && y < terrainHeight - 1) {
                double caveValue = caveNoise.octaveNoise3D(
                        x * CAVE_SCALE, y * CAVE_SCALE, z * CAVE_SCALE, 3, 0.5);
                if (caveValue > CAVE_THRESHOLD) {
                    // This block is carved out — it's a cave
                    continue; // Leave as AIR
                }
            }

            // Check for gravel deposits underground
            if (y < terrainHeight - 4) {
                double gravelValue = detailNoise.noise3D(x * 0.1, y * 0.1, z * 0.1);
                if (gravelValue > 0.55) {
                    world.setBlock(x, y, z, BlockType.GRAVEL);
                    continue;
                }
            }

            if (y == terrainHeight) {
                // Surface layer
                world.setBlock(x, y, z, surfaceBlock);
            } else if (y > terrainHeight - 3) {
                // Sub-surface (3 blocks of dirt under grass/sand, or stone under snow)
                if (surfaceBlock == BlockType.SAND) {
                    world.setBlock(x, y, z, BlockType.SAND);
                } else {
                    world.setBlock(x, y, z, BlockType.DIRT);
                }
            } else {
                // Deep underground = stone
                world.setBlock(x, y, z, BlockType.STONE);
            }
        }

        // Step 4: Fill water in low areas
        for (int y = 1; y <= WATER_LEVEL; y++) {
            if (world.getBlock(x, y, z) == BlockType.AIR) {
                world.setBlock(x, y, z, BlockType.WATER);
            }
        }
    }

    /**
     * Decide what block goes on the surface based on height and biome.
     */
    private BlockType determineSurfaceBlock(int height, double biomeValue) {
        // High altitude = snow (mountain peaks)
        if (height > BASE_HEIGHT + HEIGHT_VARIATION * 0.5) {
            return BlockType.SNOW;
        }
        // Near or below water level = sand (beaches)
        if (height <= WATER_LEVEL + 2) {
            return BlockType.SAND;
        }
        // Cold biome regions also get snow
        if (biomeValue > 0.4 && height > BASE_HEIGHT + 3) {
            return BlockType.SNOW;
        }
        // Default = grass
        return BlockType.GRASS;
    }
}
