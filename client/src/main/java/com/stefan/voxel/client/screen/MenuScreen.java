package com.stefan.voxel.client.screen;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class MenuScreen implements Screen {
    private final ScreenManager screenManager;
    private final int width;
    private final int height;
    private boolean startGame = false;

    public MenuScreen(ScreenManager screenManager, int width, int height) {
        this.screenManager = screenManager;
        this.width = width;
        this.height = height;
    }

    @Override
    public void init() {
        System.out.println("Menu Screen Initialized. Press SPACE to start New Game.");
    }

    @Override
    public void update(float deltaTime) {
        // Simple input handling for now: press SPACE to start game
        // In a real game, we'd have a UI library (like Dear ImGui or a custom one)
        long window = GLFW.glfwGetCurrentContext();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            startGame = true;
        }

        if (startGame) {
            screenManager.setScreen(new GameScreen(width, height));
        }
    }

    @Override
    public void render() {
        // Clear to a different color (dark gray) for the menu
        GL11.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void cleanup() {
        System.out.println("Exiting Menu Screen.");
    }
}
