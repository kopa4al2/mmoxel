package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.Window;
import com.stefan.voxel.client.physics.PlayerPhysics;
import com.stefan.voxel.client.render.BlockRaycaster;
import com.stefan.voxel.client.render.FPSCamera;
import com.stefan.voxel.client.render.opengl.GuiRenderer;
import com.stefan.voxel.client.render.opengl.OpenGLRenderer;
import org.joml.Vector3f;
import com.stefan.voxel.core.storage.WorldInfo;
import com.stefan.voxel.core.storage.WorldStorage;
import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.Structure;
import com.stefan.voxel.core.world.World;
import com.stefan.voxel.core.world.WorldGenerator;
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
    private final FPSCamera camera;
    private final PlayerPhysics physics;
    private final WorldStorage storage;
    private final String worldName;
    private final long seed;
    private final boolean isNewWorld;
    private GuiRenderer crosshairRenderer;
    private Vector3i highlightedBlock;

    private boolean leftMouseWasPressed = false;
    private boolean rightMouseWasPressed = false;

    /**
     * @param window    the game window
     * @param storage   persistence backend (may be null for no saving)
     * @param worldName name used for saving/loading
     * @param seed      world generation seed (used only for new worlds)
     * @param isNewWorld true = generate new world, false = load from storage
     */
    public GameScreen(Window window, WorldStorage storage, String worldName, long seed, boolean isNewWorld) {
        this.window = window;
        this.storage = storage;
        this.worldName = worldName;
        this.seed = seed;
        this.isNewWorld = isNewWorld;
        this.renderer = new OpenGLRenderer();
        this.world = new World();
        this.projection = new Matrix4f();
        this.view = new Matrix4f();
        this.camera = new FPSCamera();
        this.physics = new PlayerPhysics();
    }

    @Override
    public void init() {
        renderer.init();
        crosshairRenderer = new GuiRenderer();

        if (isNewWorld) {
            // Generate a brand new world
            WorldGenerator generator = new WorldGenerator(seed);
            System.out.println("Generating world with seed: " + generator.getSeed());
            generator.generate(world, -32, -32, 64, 64);
            System.out.println("World generation complete. Chunks: " + world.getChunks().size());

            // Spawn the player on top of the terrain at block center
            int highestBlock = world.getHighestBlock(0, 0);
            float spawnY = (highestBlock >= 0) ? highestBlock + 1.01f : 50.0f;
            physics.setPosition(0.5f, spawnY, 0.5f);
        } else {
            // Load existing world from storage
            World loaded = storage.load(worldName);
            if (loaded != null) {
                // Copy chunks into our world
                world.getChunks().putAll(loaded.getChunks());
            }
            WorldInfo info = storage.loadInfo(worldName);
            if (info != null && (info.getPlayerX() != 0 || info.getPlayerY() != 0 || info.getPlayerZ() != 0)) {
                physics.setPosition(info.getPlayerX(), info.getPlayerY(), info.getPlayerZ());
            } else {
                int highestBlock = world.getHighestBlock(0, 0);
                float spawnY = (highestBlock >= 0) ? highestBlock + 1.01f : 50.0f;
                physics.setPosition(0.5f, spawnY, 0.5f);
            }
        }

        // Set camera to eye position (visual/scaled space)
        float S = OpenGLRenderer.BLOCK_SCALE;
        Vector3f eye = physics.getEyePosition();
        camera.setPosition(eye.x * S, eye.y * S, eye.z * S);

        // Capture the mouse cursor for FPS-style look
        camera.captureCursor(window.getHandle());

        updateProjection();
    }

    private void updateProjection() {
        float aspect = (float) window.getWidth() / window.getHeight();
        projection.setPerspective((float) Math.toRadians(70.0f), aspect, 0.1f, 1000.0f);
    }

    @Override
    public void update(float deltaTime) {
        updateProjection();

        long handle = window.getHandle();

        // Handle mouse look (updates yaw/pitch only)
        camera.handleInput(handle, deltaTime);

        // Get movement input from camera (WASD relative to yaw, in world coords)
        float[] move = camera.getMovementInput(handle, deltaTime);
        boolean jump = camera.isJumpPressed(handle);

        // Update physics (gravity, collision, jumping) in world coordinates
        physics.update(world, move[0], move[1], jump, deltaTime);

        // Sync camera position to physics eye position (converted to visual/scaled space)
        float S = OpenGLRenderer.BLOCK_SCALE;
        Vector3f eye = physics.getEyePosition();
        camera.setPosition(eye.x * S, eye.y * S, eye.z * S);

        camera.applyViewMatrix(view);

        // Convert camera position from visual (scaled) space to logical (world) space for raycasting
        Vector3f logicalPos = camera.getCameraPosition().div(S);
        BlockRaycaster.RaycastResult rayResult = BlockRaycaster.raycastDetailed(
                world, logicalPos, camera.getCameraDirection());
        highlightedBlock = rayResult != null ? rayResult.blockPos() : null;

        // Handle block interaction (only when cursor is captured and not just recaptured)
        if (camera.isCursorCaptured() && !camera.wasJustRecaptured()) {
            handleBlockInteraction(rayResult);
        } else {
            // Reset mouse state so we don't trigger on the next capture
            leftMouseWasPressed = GLFW.glfwGetMouseButton(window.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            rightMouseWasPressed = GLFW.glfwGetMouseButton(window.getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        }
    }

    private void handleBlockInteraction(BlockRaycaster.RaycastResult rayResult) {
        long handle = window.getHandle();
        boolean leftPressed = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightPressed = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // Left click release — remove block (or entire structure if it belongs to one)
        if (!leftPressed && leftMouseWasPressed && rayResult != null) {
            Vector3i pos = rayResult.blockPos();
            BlockType existing = world.getBlock(pos.x, pos.y, pos.z);
            if (existing != BlockType.BEDROCK) {
                Structure structure = world.getStructureAt(pos.x, pos.y, pos.z);
                if (structure != null) {
                    world.removeStructure(structure);
                } else {
                    world.setBlock(pos.x, pos.y, pos.z, BlockType.AIR);
                }
                renderer.rebuildAllMeshes(world);
                saveWorld();
            }
        }

        // Right click release — place grass block on the adjacent face
        if (!rightPressed && rightMouseWasPressed && rayResult != null) {
            Vector3i adj = rayResult.adjacentPos();
            if (world.getBlock(adj.x, adj.y, adj.z) == BlockType.AIR) {
                world.setBlock(adj.x, adj.y, adj.z, BlockType.GRASS);
                renderer.rebuildAllMeshes(world);
                saveWorld();
            }
        }

        leftMouseWasPressed = leftPressed;
        rightMouseWasPressed = rightPressed;
    }

    /**
     * Persist the current world state to storage.
     */
    private void saveWorld() {
        if (storage == null) return;
        WorldInfo info = new WorldInfo(worldName, seed);
        Vector3f pos = physics.getPosition();
        info.setPlayerPosition(pos.x, pos.y, pos.z);
        storage.save(info, world);
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
        // Auto-save on exit
        saveWorld();
        renderer.cleanup();
        if (crosshairRenderer != null) crosshairRenderer.cleanup();
    }
}
