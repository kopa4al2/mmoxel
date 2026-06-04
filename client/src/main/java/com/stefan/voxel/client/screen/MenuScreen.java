package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.Window;
import com.stefan.voxel.client.render.opengl.GuiRenderer;
import com.stefan.voxel.client.render.opengl.TextRenderer;
import com.stefan.voxel.client.ui.Button;
import com.stefan.voxel.core.storage.FileWorldStorage;
import com.stefan.voxel.core.storage.WorldStorage;
import org.lwjgl.opengl.GL11;

public class MenuScreen implements Screen {
    private final ScreenManager screenManager;
    private final Window window;
    private final WorldStorage storage;

    private GuiRenderer guiRenderer;
    private TextRenderer textRenderer;
    private Button newGameButton;
    private Button loadGameButton;

    private boolean goNewGame = false;
    private boolean goLoadGame = false;

    public MenuScreen(ScreenManager screenManager, Window window) {
        this.screenManager = screenManager;
        this.window = window;
        this.storage = new FileWorldStorage();
    }

    @Override
    public void init() {
        System.out.println("Menu Screen Initialized.");
        guiRenderer = new GuiRenderer();
        textRenderer = new TextRenderer();
        newGameButton = new Button(0, 0.1f, 0.3f, 0.1f, "NEW GAME", () -> goNewGame = true);
        loadGameButton = new Button(0, -0.15f, 0.3f, 0.1f, "LOAD GAME", () -> goLoadGame = true);
    }

    @Override
    public void update(float deltaTime) {
        long handle = window.getHandle();
        newGameButton.update(handle, window.getWidth(), window.getHeight());
        loadGameButton.update(handle, window.getWidth(), window.getHeight());

        if (goNewGame) {
            screenManager.setScreen(new NewGameScreen(screenManager, window, storage));
            return;
        }
        if (goLoadGame) {
            screenManager.setScreen(new LoadGameScreen(screenManager, window, storage));
        }
    }

    @Override
    public void render() {
        GL11.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        float textScale = 0.002f;

        // Title
        String title = "VOXEL GAME";
        float tw = textRenderer.getTextWidth(title, 0.003f);
        textRenderer.drawText(title, -tw / 2, 0.6f, 0.003f, 1f, 1f, 1f);

        // New Game button
        renderButton(newGameButton, textScale);

        // Load Game button
        renderButton(loadGameButton, textScale);
    }

    private void renderButton(Button btn, float textScale) {
        float r = btn.isHovered() ? 0.5f : 0.4f;
        guiRenderer.drawRect(btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), r, 0.4f, 0.4f, 1.0f);

        String text = btn.getText();
        float btw = textRenderer.getTextWidth(text, textScale);
        float bth = textRenderer.getTextHeight(text, textScale);
        float tx = btn.getX() - btw / 2.0f;
        float ty = btn.getY() - bth / 2.0f;
        textRenderer.drawText(text, tx, ty, textScale, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void cleanup() {
        System.out.println("Exiting Menu Screen.");
        if (guiRenderer != null) guiRenderer.cleanup();
        if (textRenderer != null) textRenderer.cleanup();
    }
}
