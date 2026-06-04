package com.stefan.voxel.client.ui;

import org.lwjgl.glfw.GLFW;

public class Button {
    private final float x, y, width, height;
    private boolean hovered;
    private boolean clicked;
    private final String text;
    private final Runnable onClick;

    public Button(float x, float y, float width, float height, String text, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.onClick = onClick;
    }

    public void update(long windowHandle, int screenWidth, int screenHeight) {
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, mouseX, mouseY);

        // Convert mouse coordinates to NDC (-1 to 1)
        float nx = (float) (2.0 * mouseX[0] / screenWidth - 1.0);
        float ny = (float) (1.0 - 2.0 * mouseY[0] / screenHeight);

        hovered = nx >= x - width && nx <= x + width &&
                  ny >= y - height && ny <= y + height;

        if (hovered && GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            clicked = true;
        } else if (clicked && GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_RELEASE) {
            clicked = false;
            onClick.run();
        }
    }

    public boolean isHovered() {
        return hovered;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getText() { return text; }
}
