package com.stefan.voxel.client.render;

import com.stefan.voxel.core.world.World;
import org.joml.Matrix4f;

public interface Renderer {
    void init();
    void render(World world, Matrix4f projection, Matrix4f view);
    void cleanup();
}
