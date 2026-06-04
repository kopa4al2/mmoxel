package com.stefan.voxel.client.render;

import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * DDA-based voxel raycaster to find the block the player is looking at.
 */
public class BlockRaycaster {

    private static final float MAX_DISTANCE = 100.0f;

  /**
   * Result of a raycast, containing the hit block position and the adjacent
   * position (the empty block on the face that was hit — used for block placement).
   */
  public record RaycastResult(Vector3i blockPos, Vector3i adjacentPos) {}
    /**
     * Cast a ray from origin in direction and return the first non-air block hit.
     * Returns null if no block is hit within MAX_DISTANCE.
     */
    public static Vector3i raycast(World world, Vector3f origin, Vector3f direction) {
        RaycastResult result = raycastDetailed(world, origin, direction);
        return result != null ? result.blockPos : null;
    }

    /**
     * Cast a ray and return both the hit block and the adjacent empty block
     * (for block placement on the face that was hit).
     */
    public static RaycastResult raycastDetailed(World world, Vector3f origin, Vector3f direction) {
        float dirX = direction.x;
        float dirY = direction.y;
        float dirZ = direction.z;

        // Current voxel position
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        // Step direction
        int stepX = dirX >= 0 ? 1 : -1;
        int stepY = dirY >= 0 ? 1 : -1;
        int stepZ = dirZ >= 0 ? 1 : -1;

        // tDelta: how far along the ray we must move for each component to cross a voxel boundary
        float tDeltaX = dirX == 0 ? Float.MAX_VALUE : Math.abs(1.0f / dirX);
        float tDeltaY = dirY == 0 ? Float.MAX_VALUE : Math.abs(1.0f / dirY);
        float tDeltaZ = dirZ == 0 ? Float.MAX_VALUE : Math.abs(1.0f / dirZ);

        // tMax: distance to next voxel boundary for each axis
        float tMaxX = dirX == 0 ? Float.MAX_VALUE :
                (dirX > 0 ? (x + 1 - origin.x) : (origin.x - x)) * tDeltaX;
        float tMaxY = dirY == 0 ? Float.MAX_VALUE :
                (dirY > 0 ? (y + 1 - origin.y) : (origin.y - y)) * tDeltaY;
        float tMaxZ = dirZ == 0 ? Float.MAX_VALUE :
                (dirZ > 0 ? (z + 1 - origin.z) : (origin.z - z)) * tDeltaZ;

        // Track the previous position (the empty block before the hit)
        int prevX = x, prevY = y, prevZ = z;

        float dist = 0;
        while (dist < MAX_DISTANCE) {
            BlockType block = world.getBlock(x, y, z);
            if (block != BlockType.AIR) {
                return new RaycastResult(
                        new Vector3i(x, y, z),
                        new Vector3i(prevX, prevY, prevZ)
                );
            }

            // Save current position as "previous" before stepping
            prevX = x;
            prevY = y;
            prevZ = z;

            // Advance along the axis with the smallest tMax
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return null;
    }
}
