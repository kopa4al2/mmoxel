package com.stefan.voxel.core.world;

import java.util.Random;

/**
 * A simple 2D/3D Perlin noise implementation used for procedural terrain generation.
 *
 * How Perlin noise works (simplified):
 * 1. We create a shuffled permutation table (256 entries) from a seed — this is what makes
 *    the same seed always produce the same terrain.
 * 2. For any input coordinate (x, z), we find which "grid cell" it falls in.
 * 3. At each corner of that cell, we compute a pseudo-random gradient vector (derived from
 *    the permutation table).
 * 4. We calculate dot products between those gradients and the vector from each corner to
 *    our point.
 * 5. We smoothly interpolate (using a "fade" curve) between those dot products.
 * 6. The result is a smooth, continuous value between roughly -1 and 1 that varies naturally
 *    across space — perfect for terrain heights, cave density, biome blending, etc.
 *
 * "Octaves" layer multiple noise samples at different frequencies and amplitudes to create
 * more natural-looking terrain (big hills + small bumps).
 */
public class PerlinNoise {
    private final int[] permutation;

    public PerlinNoise(long seed) {
        permutation = new int[512];
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        // Shuffle using the seed — same seed = same shuffle = same terrain
        Random random = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        // Double the table so we don't need to worry about index wrapping
        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    /**
     * Sample 2D Perlin noise at (x, z). Returns a value roughly in [-1, 1].
     */
    public double noise2D(double x, double z) {
        // Find the grid cell
        int xi = (int) Math.floor(x) & 255;
        int zi = (int) Math.floor(z) & 255;

        // Position within the cell (0.0 to 1.0)
        double xf = x - Math.floor(x);
        double zf = z - Math.floor(z);

        // Smooth interpolation curves (6t^5 - 15t^4 + 10t^3)
        // This prevents visible grid artifacts
        double u = fade(xf);
        double v = fade(zf);

        // Hash the 4 corners of the cell using the permutation table
        int aa = permutation[permutation[xi] + zi];
        int ab = permutation[permutation[xi] + zi + 1];
        int ba = permutation[permutation[xi + 1] + zi];
        int bb = permutation[permutation[xi + 1] + zi + 1];

        // Compute gradient dot products at each corner, then interpolate
        double x1 = lerp(grad2D(aa, xf, zf), grad2D(ba, xf - 1, zf), u);
        double x2 = lerp(grad2D(ab, xf, zf - 1), grad2D(bb, xf - 1, zf - 1), u);

        return lerp(x1, x2, v);
    }

    /**
     * Sample 3D Perlin noise at (x, y, z). Used for caves and ore placement.
     */
    public double noise3D(double x, double y, double z) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        int zi = (int) Math.floor(z) & 255;

        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double zf = z - Math.floor(z);

        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        int aaa = permutation[permutation[permutation[xi] + yi] + zi];
        int aba = permutation[permutation[permutation[xi] + yi + 1] + zi];
        int aab = permutation[permutation[permutation[xi] + yi] + zi + 1];
        int abb = permutation[permutation[permutation[xi] + yi + 1] + zi + 1];
        int baa = permutation[permutation[permutation[xi + 1] + yi] + zi];
        int bba = permutation[permutation[permutation[xi + 1] + yi + 1] + zi];
        int bab = permutation[permutation[permutation[xi + 1] + yi] + zi + 1];
        int bbb = permutation[permutation[permutation[xi + 1] + yi + 1] + zi + 1];

        double x1 = lerp(grad3D(aaa, xf, yf, zf), grad3D(baa, xf - 1, yf, zf), u);
        double x2 = lerp(grad3D(aba, xf, yf - 1, zf), grad3D(bba, xf - 1, yf - 1, zf), u);
        double y1 = lerp(x1, x2, v);

        x1 = lerp(grad3D(aab, xf, yf, zf - 1), grad3D(bab, xf - 1, yf, zf - 1), u);
        x2 = lerp(grad3D(abb, xf, yf - 1, zf - 1), grad3D(bbb, xf - 1, yf - 1, zf - 1), u);
        double y2 = lerp(x1, x2, v);

        return lerp(y1, y2, w);
    }

    /**
     * Layered noise ("fractal Brownian motion") — combines multiple octaves for richer terrain.
     * Each octave doubles the frequency (smaller features) and halves the amplitude (less influence).
     *
     * Example with 4 octaves:
     *   Octave 1: big rolling hills      (freq=1,   amp=1.0)
     *   Octave 2: medium bumps            (freq=2,   amp=0.5)
     *   Octave 3: small ridges            (freq=4,   amp=0.25)
     *   Octave 4: tiny surface roughness  (freq=8,   amp=0.125)
     */
    public double octaveNoise2D(double x, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0; // Used to normalize the result to [-1, 1]

        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    public double octaveNoise3D(double x, double y, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }

    // Smooth interpolation curve: 6t^5 - 15t^4 + 10t^3
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // Linear interpolation
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    // 2D gradient: picks a pseudo-random direction based on hash, dots it with (x, z)
    private double grad2D(int hash, double x, double z) {
        int h = hash & 3;
        return switch (h) {
            case 0 -> x + z;
            case 1 -> -x + z;
            case 2 -> x - z;
            case 3 -> -x - z;
            default -> 0;
        };
    }

    // 3D gradient
    private double grad3D(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
