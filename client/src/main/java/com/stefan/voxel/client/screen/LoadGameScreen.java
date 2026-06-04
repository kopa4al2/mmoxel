package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.Window;
import com.stefan.voxel.client.render.opengl.GuiRenderer;
import com.stefan.voxel.client.render.opengl.TextRenderer;
import com.stefan.voxel.client.ui.Button;
import com.stefan.voxel.core.storage.WorldStorage;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen that lists saved worlds and lets the player load one.
 */
public class LoadGameScreen implements Screen {
    private final ScreenManager screenManager;
    private final Window window;
    private final WorldStorage storage;

    private GuiRenderer guiRenderer;
    private TextRenderer textRenderer;
    private Button backButton;
    private final List<Button> worldButtons = new ArrayList<>();
    private final List<String> worldNames = new ArrayList<>();

    private boolean goBack = false;
    private String selectedWorld = null;

    public LoadGameScreen(ScreenManager screenManager, Window window, WorldStorage storage) {
        this.screenManager = screenManager;
        this.window = window;
        this.storage = storage;
    }

    @Override
    public void init() {
        guiRenderer = new GuiRenderer();
        textRenderer = new TextRenderer();

        backButton = new Button(0, -0.7f, 0.25f, 0.08f, "BACK", () -> goBack = true);

        // List saved worlds and create a button for each
        List<String> saved = storage.listWorlds();
        float startY = 0.4f;
        float spacing = 0.2f;
        for (int i = 0; i < saved.size(); i++) {
            String name = saved.get(i);
            worldNames.add(name);
            float y = startY - i * spacing;
            worldButtons.add(new Button(0, y, 0.35f, 0.07f, name, () -> selectedWorld = name));
        }
    }

    @Override
    public void update(float deltaTime) {
        long handle = window.getHandle();
        backButton.update(handle, window.getWidth(), window.getHeight());
        for (Button btn : worldButtons) {
            btn.update(handle, window.getWidth(), window.getHeight());
        }

        if (goBack) {
            screenManager.setScreen(new MenuScreen(screenManager, window));
            return;
        }

        if (selectedWorld != null) {
            screenManager.setScreen(new GameScreen(window, storage, selectedWorld, 0, false));
        }
    }

    @Override
    public void render() {
        GL11.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        float textScale = 0.0018f;

        // Title
        String title = "LOAD GAME";
        float tw = textRenderer.getTextWidth(title, textScale);
        textRenderer.drawText(title, -tw / 2, 0.7f, textScale, 1f, 1f, 1f);

        if (worldButtons.isEmpty()) {
            String msg = "NO SAVED WORLDS";
            float mw = textRenderer.getTextWidth(msg, 0.0015f);
            textRenderer.drawText(msg, -mw / 2, 0.2f, 0.0015f, 0.6f, 0.6f, 0.6f);
        }

        // World buttons
        for (Button btn : worldButtons) {
            float r = btn.isHovered() ? 0.5f : 0.35f;
            guiRenderer.drawRect(btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), r, 0.4f, 0.4f, 1f);
            String text = btn.getText();
            float btw = textRenderer.getTextWidth(text, textScale);
            float bth = textRenderer.getTextHeight(text, textScale);
            textRenderer.drawText(text, btn.getX() - btw / 2, btn.getY() - bth / 2, textScale, 1f, 1f, 1f);
        }

        // Back button
        float r = backButton.isHovered() ? 0.5f : 0.4f;
        guiRenderer.drawRect(backButton.getX(), backButton.getY(), backButton.getWidth(), backButton.getHeight(), r, 0.4f, 0.4f, 1f);
        String bt = backButton.getText();
        float btw = textRenderer.getTextWidth(bt, textScale);
        float bth = textRenderer.getTextHeight(bt, textScale);
        textRenderer.drawText(bt, backButton.getX() - btw / 2, backButton.getY() - bth / 2, textScale, 1f, 1f, 1f);
    }

    @Override
    public void cleanup() {
        if (guiRenderer != null) guiRenderer.cleanup();
        if (textRenderer != null) textRenderer.cleanup();
    }
}
