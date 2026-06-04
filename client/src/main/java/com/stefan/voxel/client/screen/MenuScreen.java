package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.Window;
import com.stefan.voxel.client.render.opengl.GuiRenderer;
import com.stefan.voxel.client.render.opengl.TextRenderer;
import com.stefan.voxel.client.ui.Button;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class MenuScreen implements Screen {
    private final ScreenManager screenManager;
    private final Window window;
    private boolean startGame = false;
    private GuiRenderer guiRenderer;
    private TextRenderer textRenderer;
    private Button newGameButton;

    public MenuScreen(ScreenManager screenManager, Window window) {
        this.screenManager = screenManager;
        this.window = window;
    }

    @Override
    public void init() {
        System.out.println("Menu Screen Initialized.");
        guiRenderer = new GuiRenderer();
        textRenderer = new TextRenderer();
        newGameButton = new Button(0, 0, 0.3f, 0.1f, "START", () -> startGame = true);
    }

    @Override
    public void update(float deltaTime) {
        newGameButton.update(window.getHandle(), window.getWidth(), window.getHeight());

        if (startGame) {
            screenManager.setScreen(new GameScreen(window));
        }
    }

    @Override
    public void render() {
        // Clear to a different color (dark gray) for the menu
        GL11.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        float r = newGameButton.isHovered() ? 0.5f : 0.4f;
        guiRenderer.drawRect(newGameButton.getX(), newGameButton.getY(), 
                            newGameButton.getWidth(), newGameButton.getHeight(), 
                            r, 0.4f, 0.4f, 1.0f);

        // Draw text centered on button
        float textScale = 0.002f;
        String text = newGameButton.getText();
        float textWidth = textRenderer.getTextWidth(text, textScale);
        float textHeight = textRenderer.getTextHeight(text, textScale);
        
        // Button center is (newGameButton.getX(), newGameButton.getY()) in NDC
        // Text is drawn from top-left(?) wait, let's check TextRenderer.drawText
        // xpos = currentX; ypos = y; float w = glyph.width * scale; float h = glyph.height * scale;
        // vertices: xpos, ypos+h; xpos, ypos; xpos+w, ypos; ...
        // So (x, y) is the bottom-left of the text.
        
        float tx = newGameButton.getX() - textWidth / 2.0f;
        float ty = newGameButton.getY() - textHeight / 2.0f;

        textRenderer.drawText(text, tx, ty, textScale, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void cleanup() {
        System.out.println("Exiting Menu Screen.");
        if (guiRenderer != null) {
            guiRenderer.cleanup();
        }
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
    }
}
