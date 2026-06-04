package com.stefan.voxel.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class OrbitCamera {
    private final Vector3f target = new Vector3f(0, 0, 0);
    private float distance = 30.0f;
    private float yaw = 0.0f;   // horizontal angle in radians
    private float pitch = 0.3f; // vertical angle in radians

    private static final float MIN_PITCH = 0.05f;
    private static final float MAX_PITCH = (float) (Math.PI / 2.0 - 0.05);
    private static final float MIN_DISTANCE = 5.0f;
    private static final float MAX_DISTANCE = 200.0f;
    private static final float MOVE_SPEED = 20.0f;
    private static final float MOUSE_SENSITIVITY = 0.005f;
    private static final float ZOOM_SPEED = 2.0f;

    private double lastMouseX, lastMouseY;
    private boolean dragging = false;
    private boolean firstMouse = true;

    public void handleInput(long window, float deltaTime) {
        // WASD to move the target point
        float forward = 0, right = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) forward += 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) forward -= 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) right -= 1;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) right += 1;

        if (forward != 0 || right != 0) {
            float speed = MOVE_SPEED * deltaTime;
            // Move relative to camera yaw direction
            float sinYaw = (float) Math.sin(yaw);
            float cosYaw = (float) Math.cos(yaw);
            target.x += (-sinYaw * forward + cosYaw * right) * speed;
            target.z += (-cosYaw * forward - sinYaw * right) * speed;
        }

        // Q/E to move target up/down
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Q) == GLFW.GLFW_PRESS) target.y -= MOVE_SPEED * deltaTime;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_E) == GLFW.GLFW_PRESS) target.y += MOVE_SPEED * deltaTime;

        // Right mouse button to rotate
        double[] mx = new double[1], my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);

        boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (rmb) {
            if (firstMouse) {
                lastMouseX = mx[0];
                lastMouseY = my[0];
                firstMouse = false;
            }
            double dx = mx[0] - lastMouseX;
            double dy = my[0] - lastMouseY;
            yaw += (float) dx * MOUSE_SENSITIVITY;
            pitch += (float) dy * MOUSE_SENSITIVITY;
            pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
        } else {
            firstMouse = true;
        }
        lastMouseX = mx[0];
        lastMouseY = my[0];

        // Scroll to zoom (handled via callback, but we can also use +/- keys)
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_MINUS) == GLFW.GLFW_PRESS) distance += ZOOM_SPEED;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_EQUAL) == GLFW.GLFW_PRESS) distance -= ZOOM_SPEED;
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    public void zoom(float amount) {
        distance -= amount * ZOOM_SPEED;
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    public void applyViewMatrix(Matrix4f view) {
        Vector3f pos = getCameraPosition();
        view.identity().lookAt(pos.x, pos.y, pos.z, target.x, target.y, target.z, 0, 1, 0);
    }

    public Vector3f getCameraPosition() {
        float x = target.x + distance * (float) (Math.cos(pitch) * Math.sin(yaw));
        float y = target.y + distance * (float) Math.sin(pitch);
        float z = target.z + distance * (float) (Math.cos(pitch) * Math.cos(yaw));
        return new Vector3f(x, y, z);
    }

    public Vector3f getCameraDirection() {
        Vector3f pos = getCameraPosition();
        return new Vector3f(target.x - pos.x, target.y - pos.y, target.z - pos.z).normalize();
    }
}
