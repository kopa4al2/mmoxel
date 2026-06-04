package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.Window;
import com.stefan.voxel.client.render.opengl.GuiRenderer;
import com.stefan.voxel.client.render.opengl.TextRenderer;
import com.stefan.voxel.client.ui.Button;
import com.stefan.voxel.client.ui.TextInput;
import com.stefan.voxel.core.storage.WorldStorage;
import org.lwjgl.opengl.GL11;

/**
 * Screen where the player enters a world name and seed, then starts a new game.
 */
public class NewGameScreen implements Screen {
    private final ScreenManager screenManager;
    private final Window window;
    private final WorldStorage storage;

    private GuiRenderer guiRenderer;
    private TextRenderer textRenderer;

    private TextInput nameInput;
    private TextInput seedInput;
    private Button createButton;
    private Button backButton;

    private boolean goCreate = false;
    private boolean goBack = false;

    /** Which field is active: 0 = name, 1 = seed */
    private int activeField = 0;

    public NewGameScreen(ScreenManager screenManager, Window window, WorldStorage storage) {
        this.screenManager = screenManager;
        this.window = window;
        this.storage = storage;
    }

    @Override
    public void init() {
        guiRenderer = new GuiRenderer();
        textRenderer = new TextRenderer();

        nameInput = new TextInput(32);
        nameInput.setText("MyWorld");
        nameInput.attach(window.getHandle());

        seedInput = new TextInput(20);
        seedInput.setText("42");
        seedInput.setActive(false);

        createButton = new Button(0, -0.3f, 0.25f, 0.08f, "CREATE", () -> goCreate = true);
        backButton = new Button(0, -0.5f, 0.25f, 0.08f, "BACK", () -> goBack = true);
    }

    @Override
    public void update(float deltaTime) {
        long handle = window.getHandle();
        createButton.update(handle, window.getWidth(), window.getHeight());
        backButton.update(handle, window.getWidth(), window.getHeight());

        // Tab to switch fields
        // We check for TAB press via polling (simple approach)
        // The TextInput char callback won't capture TAB, so we poll it here
        // We use a simple toggle on TAB press
        if (org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            if (activeField == 0) {
                activeField = 1;
                nameInput.detach(handle);
                nameInput.setActive(false);
                seedInput.setActive(true);
                seedInput.attach(handle);
            } else {
                activeField = 0;
                seedInput.detach(handle);
                seedInput.setActive(false);
                nameInput.setActive(true);
                nameInput.attach(handle);
            }
        }

        if (goBack) {
            cleanupInputs();
            screenManager.setScreen(new MenuScreen(screenManager, window));
            return;
        }

        if (goCreate) {
            String worldName = nameInput.getText().trim();
            if (worldName.isEmpty()) worldName = "MyWorld";

            long seed;
            try {
                seed = Long.parseLong(seedInput.getText().trim());
            } catch (NumberFormatException e) {
                // Use hash of the string as seed
                seed = seedInput.getText().trim().hashCode();
            }

            cleanupInputs();
            screenManager.setScreen(new GameScreen(window, storage, worldName, seed, true));
        }
    }

    private void cleanupInputs() {
        long handle = window.getHandle();
        if (activeField == 0) {
            nameInput.detach(handle);
        } else {
            seedInput.detach(handle);
        }
    }

    @Override
    public void render() {
        GL11.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        float textScale = 0.0018f;
        float labelScale = 0.0015f;

        // Title
        String title = "NEW GAME";
        float tw = textRenderer.getTextWidth(title, textScale);
        textRenderer.drawText(title, -tw / 2, 0.6f, textScale, 1f, 1f, 1f);

        // World Name label + input box
        textRenderer.drawText("WORLD NAME:", -0.4f, 0.35f, labelScale, 0.8f, 0.8f, 0.8f);
        float nameBoxColor = activeField == 0 ? 0.5f : 0.35f;
        guiRenderer.drawRect(0, 0.2f, 0.4f, 0.06f, nameBoxColor, nameBoxColor, nameBoxColor, 1f);
        String nameText = nameInput.getText() + (activeField == 0 ? "_" : "");
        float ntw = textRenderer.getTextWidth(nameText, labelScale);
        textRenderer.drawText(nameText, -ntw / 2, 0.2f - textRenderer.getTextHeight(nameText, labelScale) / 2, labelScale, 1f, 1f, 1f);

        // Seed label + input box
        textRenderer.drawText("SEED:", -0.4f, 0.05f, labelScale, 0.8f, 0.8f, 0.8f);
        float seedBoxColor = activeField == 1 ? 0.5f : 0.35f;
        guiRenderer.drawRect(0, -0.1f, 0.4f, 0.06f, seedBoxColor, seedBoxColor, seedBoxColor, 1f);
        String seedText = seedInput.getText() + (activeField == 1 ? "_" : "");
        float stw = textRenderer.getTextWidth(seedText, labelScale);
        textRenderer.drawText(seedText, -stw / 2, -0.1f - textRenderer.getTextHeight(seedText, labelScale) / 2, labelScale, 1f, 1f, 1f);

        // Buttons
        renderButton(createButton, textScale);
        renderButton(backButton, textScale);
    }

    private void renderButton(Button btn, float textScale) {
        float r = btn.isHovered() ? 0.5f : 0.4f;
        guiRenderer.drawRect(btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), r, 0.4f, 0.4f, 1f);
        String text = btn.getText();
        float btw = textRenderer.getTextWidth(text, textScale);
        float bth = textRenderer.getTextHeight(text, textScale);
        textRenderer.drawText(text, btn.getX() - btw / 2, btn.getY() - bth / 2, textScale, 1f, 1f, 1f);
    }

    @Override
    public void cleanup() {
        if (guiRenderer != null) guiRenderer.cleanup();
        if (textRenderer != null) textRenderer.cleanup();
    }
}
