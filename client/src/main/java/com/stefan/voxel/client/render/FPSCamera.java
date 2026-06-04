package com.stefan.voxel.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * First-person camera controlled by mouse look and WASD movement.
 * The mouse controls where the player looks (center of screen follows mouse).
 * WASD moves the player relative to the look direction.
 */
public class FPSCamera {
    private final Vector3f position = new Vector3f(0, 40, 0);
    private float yaw = 0.0f;     // horizontal angle in radians
    private float pitch = 0.0f;   // vertical angle in radians

    private static final float MAX_PITCH = (float) (Math.PI / 2.0 - 0.05);
    private static final float MOVE_SPEED = 5.0f;
    private static final float MOUSE_SENSITIVITY = 0.003f;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private boolean cursorCaptured = false;
    private boolean justRecaptured = false;

    /**
     * Call once after the window is created to capture the cursor.
     */
    public void captureCursor(long window) {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        cursorCaptured = true;
        firstMouse = true;
    }

    /**
     * Release the cursor (e.g. for menus).
     */
    public void releaseCursor(long window) {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        cursorCaptured = false;
    }

    public void handleInput(long window, float deltaTime) {
        // Mouse look
        double[] mx = new double[1], my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);

        if (firstMouse) {
            lastMouseX = mx[0];
            lastMouseY = my[0];
            firstMouse = false;
        }

        double dx = mx[0] - lastMouseX;
        double dy = my[0] - lastMouseY;
        lastMouseX = mx[0];
        lastMouseY = my[0];

        if (cursorCaptured) {
            yaw += (float) dx * MOUSE_SENSITIVITY;
            pitch -= (float) dy * MOUSE_SENSITIVITY;
            pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
        }

        // ESC to release cursor
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            releaseCursor(window);
        }
        // Click to recapture
        justRecaptured = false;
        if (!cursorCaptured && GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            captureCursor(window);
            justRecaptured = true;
        }
    }

    public void applyViewMatrix(Matrix4f view) {
        Vector3f dir = getCameraDirection();
        Vector3f target = new Vector3f(position).add(dir);
        view.identity().lookAt(position.x, position.y, position.z, target.x, target.y, target.z, 0, 1, 0);
    }

    public Vector3f getCameraPosition() {
        return new Vector3f(position);
    }

    public Vector3f getCameraDirection() {
        float x = (float) (Math.cos(pitch) * -Math.sin(yaw));
        float y = (float) Math.sin(pitch);
        float z = (float) (Math.cos(pitch) * -Math.cos(yaw));
        return new Vector3f(x, y, z).normalize();
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    /**
     * Compute the desired horizontal movement vector based on WASD input.
     * Returns a float[2] = {moveX, moveZ} in world-relative direction (based on yaw).
     * The caller (physics) applies this movement with collision.
     */
    public float[] getMovementInput(long window, float deltaTime) {
        float forward = 0, right = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) forward += 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) forward -= 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) right -= 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) right += 1;

        float speed = MOVE_SPEED * deltaTime;
        float sinYaw = (float) Math.sin(yaw);
        float cosYaw = (float) Math.cos(yaw);
        float mx = (-sinYaw * forward + cosYaw * right) * speed;
        float mz = (-cosYaw * forward - sinYaw * right) * speed;
        return new float[]{mx, mz};
    }

    /**
     * Returns true if the jump key (SPACE) is currently pressed.
     */
    public boolean isJumpPressed(long window) {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
    }

    /** Returns true on the frame the cursor was just recaptured (to suppress that click). */
    public boolean wasJustRecaptured() {
        return justRecaptured;
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }
}
