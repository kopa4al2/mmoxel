package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.Window;
import com.stefan.voxel.client.render.BlockRaycaster;
import com.stefan.voxel.client.render.OrbitCamera;
import com.stefan.voxel.client.render.opengl.GuiRenderer;
import com.stefan.voxel.client.render.opengl.OpenGLRenderer;
import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class GameScreen implements Screen {
    private final OpenGLRenderer renderer;
    private final World world;
    private final Matrix4f projection;
    private final Matrix4f view;
    private final Window window;
    private final OrbitCamera camera;
    private GuiRenderer crosshairRenderer;
    private Vector3i highlightedBlock;

    public GameScreen(Window window) {
        this.window = window;
        this.renderer = new OpenGLRenderer();
        this.world = new World();
        this.projection = new Matrix4f();
        this.view = new Matrix4f();
        this.camera = new OrbitCamera();
    }

    @Override
    public void init() {
        renderer.init();
        crosshairRenderer = new GuiRenderer();

        GLFW.glfwSetScrollCallback(window.getHandle(), (win, xOffset, yOffset) -> {
            camera.zoom((float) yOffset);
        });

        generateTerrain();
        updateProjection();
    }

    private void generateTerrain() {
        int halfSize = 32;
        for (int x = -halfSize; x < halfSize; x++) {
            for (int z = -halfSize; z < halfSize; z++) {
                // Simple terrain: height varies with a basic formula
                int height = 4 + (int) (3 * Math.sin(x * 0.1) * Math.cos(z * 0.1));

                // Stone layer: y=0 to height-3
                for (int y = 0; y < height - 2; y++) {
                    world.setBlock(x, y, z, BlockType.STONE);
                }
                // Dirt layer: height-3 to height-1
                for (int y = Math.max(0, height - 2); y < height; y++) {
                    world.setBlock(x, y, z, BlockType.DIRT);
                }
                // Grass on top
                world.setBlock(x, height, z, BlockType.GRASS);
            }
        }
    }

    private void updateProjection() {
        float aspect = (float) window.getWidth() / window.getHeight();
        projection.setPerspective((float) Math.toRadians(45.0f), aspect, 0.1f, 1000.0f);
    }

    @Override
    public void update(float deltaTime) {
        updateProjection();
        camera.handleInput(window.getHandle(), deltaTime);
        camera.applyViewMatrix(view);

        // Raycast from camera to find highlighted block
        highlightedBlock = BlockRaycaster.raycast(world, camera.getCameraPosition(), camera.getCameraDirection());
    }

    @Override
    public void render() {
        GL11.glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        renderer.render(world, projection, view);
        renderer.renderHighlight(highlightedBlock, projection, view);

        // Draw crosshair (small cross at screen center)
        float cSize = 0.015f;
        float cThick = 0.003f;
        crosshairRenderer.drawRect(0, 0, cSize, cThick, 1f, 1f, 1f, 0.8f);
        crosshairRenderer.drawRect(0, 0, cThick, cSize, 1f, 1f, 1f, 0.8f);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();
        if (crosshairRenderer != null) crosshairRenderer.cleanup();
    }
}
