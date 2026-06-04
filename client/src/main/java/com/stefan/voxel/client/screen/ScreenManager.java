package com.stefan.voxel.client.screen;

public class ScreenManager {
    private Screen currentScreen;

    public void setScreen(Screen screen) {
        if (currentScreen != null) {
            currentScreen.cleanup();
        }
        currentScreen = screen;
        if (currentScreen != null) {
            currentScreen.init();
        }
    }

    public void update(float deltaTime) {
        if (currentScreen != null) {
            currentScreen.update(deltaTime);
        }
    }

    public void render() {
        if (currentScreen != null) {
            currentScreen.render();
        }
    }

    public void cleanup() {
        if (currentScreen != null) {
            currentScreen.cleanup();
        }
    }
}
