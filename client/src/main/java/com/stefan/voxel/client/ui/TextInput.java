package com.stefan.voxel.client.ui;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * Simple single-line text input field.
 * Registers GLFW char/key callbacks to capture typed text.
 */
public class TextInput {
    private final StringBuilder text = new StringBuilder();
    private final int maxLength;
    private boolean active = true;
    private GLFWCharCallback charCallback;
    private GLFWKeyCallback keyCallback;

    public TextInput(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * Attach this input to a GLFW window to start receiving key events.
     */
    public void attach(long window) {
        charCallback = GLFW.glfwSetCharCallback(window, (win, codepoint) -> {
            if (active && text.length() < maxLength) {
                text.append((char) codepoint);
            }
        });
        keyCallback = GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (!active) return;
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && text.length() > 0) {
                    text.deleteCharAt(text.length() - 1);
                }
            }
        });
    }

    /**
     * Detach callbacks, restoring any previous ones.
     */
    public void detach(long window) {
        if (charCallback != null) {
            GLFW.glfwSetCharCallback(window, charCallback);
        } else {
            GLFW.glfwSetCharCallback(window, null);
        }
        if (keyCallback != null) {
            GLFW.glfwSetKeyCallback(window, keyCallback);
        } else {
            GLFW.glfwSetKeyCallback(window, null);
        }
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String value) {
        text.setLength(0);
        text.append(value);
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
