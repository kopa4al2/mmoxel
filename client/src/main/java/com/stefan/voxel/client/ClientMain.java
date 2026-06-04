package com.stefan.voxel.client;

import com.stefan.voxel.client.screen.MenuScreen;
import com.stefan.voxel.client.screen.ScreenManager;
import org.lwjgl.glfw.GLFW;

public class ClientMain {
    private Window window;
    private final ScreenManager screenManager = new ScreenManager();

    public void run() {
        init();
        loop();

        screenManager.cleanup();
        window.cleanup();
    }

    private void init() {
        window = new Window(800, 600, "Voxel MMO");
        screenManager.setScreen(new MenuScreen(screenManager, window));
    }

    private void loop() {
        float lastTime = (float) GLFW.glfwGetTime();

        while (!window.shouldClose()) {
            float currentTime = (float) GLFW.glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            screenManager.update(deltaTime);
            screenManager.render();

            window.update();
        }
    }

    public static void main(String[] args) {
        new ClientMain().run();
    }
}
