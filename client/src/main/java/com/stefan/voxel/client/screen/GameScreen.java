package com.stefan.voxel.client.screen;

import com.stefan.voxel.client.render.Renderer;
import com.stefan.voxel.client.render.opengl.OpenGLRenderer;
import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class GameScreen implements Screen {
    private final Renderer renderer;
    private final World world;
    private final Matrix4f projection;
    private final Matrix4f view;
    private final int width;
    private final int height;

    public GameScreen(int width, int height) {
        this.width = width;
        this.height = height;
        this.renderer = new OpenGLRenderer(); // In future, this could be injected
        this.world = new World();
        this.projection = new Matrix4f();
        this.view = new Matrix4f();
    }

    @Override
    public void init() {
        renderer.init();
        
        // Generate a small flat world
        for (int x = -16; x < 16; x++) {
            for (int z = -16; z < 16; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
                if (x % 4 == 0 && z % 4 == 0) world.setBlock(x, 1, z, BlockType.STONE);
            }
        }

        projection.perspective((float) Math.toRadians(45.0f), (float) width / height, 0.1f, 1000.0f);
    }

    @Override
    public void update(float deltaTime) {
        float time = (float) GLFW.glfwGetTime();
        view.identity()
            .lookAt(new Vector3f(20 * (float)Math.cos(time * 0.5f), 20, 20 * (float)Math.sin(time * 0.5f)),
                    new Vector3f(0, 0, 0),
                    new Vector3f(0, 1, 0));
    }

    @Override
    public void render() {
        GL11.glClearColor(0.4f, 0.6f, 0.9f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        renderer.render(world, projection, view);
    }

    @Override
    public void cleanup() {
        renderer.cleanup();
    }
}
