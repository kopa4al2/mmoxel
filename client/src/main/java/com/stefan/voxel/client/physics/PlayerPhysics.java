package com.stefan.voxel.client.physics;

import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.World;
import org.joml.Vector3f;

/**
 * Handles basic player physics: gravity, ground collision, jumping.
 * Works in logical (world) coordinates — the caller must convert to/from visual space.
 *
 * The player is modeled as an axis-aligned bounding box (AABB):
 *   width x height x width, centered horizontally on the position.
 *   Position represents the player's feet (bottom-center of the AABB).
 */
public class PlayerPhysics {

    /** Player dimensions in blocks */
    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_HEIGHT = 1.8f;
    /** Eye height offset from feet */
    public static final float EYE_HEIGHT = 1.6f;

    private static final float GRAVITY = -20.0f;       // blocks/s²
    private static final float JUMP_VELOCITY = 8.0f;   // blocks/s
    private static final float TERMINAL_VELOCITY = -50.0f;

    private final Vector3f position = new Vector3f();   // feet position (world coords)
    private float velocityY = 0.0f;
    private boolean onGround = false;

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        velocityY = 0;
        onGround = false;
    }

    /**
     * Update physics for one frame.
     * @param world the voxel world for collision checks
     * @param moveX desired horizontal movement in X (world coords)
     * @param moveZ desired horizontal movement in Z (world coords)
     * @param jump  true if the player wants to jump this frame
     * @param deltaTime seconds since last frame
     */
    public void update(World world, float moveX, float moveZ, boolean jump, float deltaTime) {
        // Jump
        if (jump && onGround) {
            velocityY = JUMP_VELOCITY;
            onGround = false;
        }

        // Apply gravity
        velocityY += GRAVITY * deltaTime;
        if (velocityY < TERMINAL_VELOCITY) velocityY = TERMINAL_VELOCITY;

        // Move each axis independently to allow sliding along walls
        moveAxis(world, moveX, 0, 0);
        moveAxis(world, 0, 0, moveZ);
        moveVertical(world, velocityY * deltaTime);
    }

    /**
     * Move horizontally along one axis, stopping at solid blocks.
     */
    private void moveAxis(World world, float dx, float dy, float dz) {
        if (dx == 0 && dz == 0) return;
        float newX = position.x + dx;
        float newZ = position.z + dz;
        if (!collidesAt(world, newX, position.y, newZ)) {
            position.x = newX;
            position.z = newZ;
        }
    }

    /**
     * Move vertically, handling ground collision and ceiling bumps.
     */
    private void moveVertical(World world, float dy) {
        float newY = position.y + dy;
        if (!collidesAt(world, position.x, newY, position.z)) {
            position.y = newY;
            onGround = false;
        } else {
            if (dy < 0) {
                // Falling — snap to top of the block we hit
                position.y = (float) Math.floor(position.y);
                onGround = true;
            } else {
                // Hit ceiling
                // Snap down slightly so we don't stick
            }
            velocityY = 0;
        }
    }

    /**
     * Check if the player AABB at the given feet position overlaps any solid block.
     */
    private boolean collidesAt(World world, float x, float y, float z) {
        float half = PLAYER_WIDTH / 2.0f;
        // AABB: [x-half, x+half] x [y, y+PLAYER_HEIGHT] x [z-half, z+half]
        int minBX = (int) Math.floor(x - half);
        int maxBX = (int) Math.floor(x + half);
        int minBY = (int) Math.floor(y);
        int maxBY = (int) Math.floor(y + PLAYER_HEIGHT);
        int minBZ = (int) Math.floor(z - half);
        int maxBZ = (int) Math.floor(z + half);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    BlockType block = world.getBlock(bx, by, bz);
                    if (block != null && block != BlockType.AIR && block != BlockType.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Get the eye position in world coordinates. */
    public Vector3f getEyePosition() {
        return new Vector3f(position.x, position.y + EYE_HEIGHT, position.z);
    }

    /** Get the feet position in world coordinates. */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public boolean isOnGround() {
        return onGround;
    }
}
