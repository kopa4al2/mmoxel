package com.stefan.voxel.client.screen;

public interface Screen {
    void init();
    void update(float deltaTime);
    void render();
    void cleanup();
}
