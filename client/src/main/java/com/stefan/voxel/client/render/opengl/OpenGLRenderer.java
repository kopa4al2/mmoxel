package com.stefan.voxel.client.render.opengl;

import com.stefan.voxel.client.render.ChunkMesh;
import com.stefan.voxel.client.render.Renderer;
import com.stefan.voxel.client.render.Shader;
import com.stefan.voxel.core.world.Chunk;
import com.stefan.voxel.core.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class OpenGLRenderer implements Renderer {
    private Shader shader;
    private Shader highlightShader;
    private final List<ChunkMesh> chunkMeshes = new ArrayList<>();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private int highlightVao;
    private int highlightVbo;
    private World lastWorld;
    private int lastChunkCount = -1;

    @Override
    public void init() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        shader = new Shader("shaders/opengl/world/world.vert", "shaders/opengl/world/world.frag");
        highlightShader = new Shader("shaders/opengl/world/highlight.vert", "shaders/opengl/world/highlight.frag");
        initHighlightMesh();
    }

    private void initHighlightMesh() {
        // Wireframe cube from (0,0,0) to (1,1,1) using GL_LINES
        float s = -0.001f; // slight inset to avoid z-fighting
        float e = 1.001f;
        float[] lines = {
            // Bottom face edges
            s,s,s, e,s,s,  e,s,s, e,s,e,  e,s,e, s,s,e,  s,s,e, s,s,s,
            // Top face edges
            s,e,s, e,e,s,  e,e,s, e,e,e,  e,e,e, s,e,e,  s,e,e, s,e,s,
            // Vertical edges
            s,s,s, s,e,s,  e,s,s, e,e,s,  e,s,e, e,e,e,  s,s,e, s,e,e,
        };

        highlightVao = GL30.glGenVertexArrays();
        highlightVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(highlightVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, highlightVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, lines, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
    }

    @Override
    public void render(World world, Matrix4f projection, Matrix4f view) {
        shader.checkHotReload();
        highlightShader.checkHotReload();

        // Rebuild meshes when chunks change
        if (lastWorld != world || chunkMeshes.size() != world.getChunks().size()) {
            rebuildAllMeshes(world);
            lastWorld = world;
        }

        shader.bind();
        GL20.glUniformMatrix4fv(shader.getUniformLocation("projection"), false, projection.get(matrixBuffer));
        GL20.glUniformMatrix4fv(shader.getUniformLocation("view"), false, view.get(matrixBuffer));

        for (ChunkMesh mesh : chunkMeshes) {
            Matrix4f model = new Matrix4f();
            Vector3i cpos = mesh.getChunk().getPosition();
            model.translate(cpos.x * Chunk.SIZE, cpos.y * Chunk.SIZE, cpos.z * Chunk.SIZE);
            GL20.glUniformMatrix4fv(shader.getUniformLocation("model"), false, model.get(matrixBuffer));
            mesh.render();
        }
        shader.unbind();
    }

    public void renderHighlight(Vector3i blockPos, Matrix4f projection, Matrix4f view) {
        if (blockPos == null) return;

        highlightShader.bind();
        GL20.glUniformMatrix4fv(highlightShader.getUniformLocation("projection"), false, projection.get(matrixBuffer));
        GL20.glUniformMatrix4fv(highlightShader.getUniformLocation("view"), false, view.get(matrixBuffer));

        Matrix4f model = new Matrix4f().translate(blockPos.x, blockPos.y, blockPos.z);
        GL20.glUniformMatrix4fv(highlightShader.getUniformLocation("model"), false, model.get(matrixBuffer));

        GL11.glLineWidth(2.0f);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL30.glBindVertexArray(highlightVao);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);
        GL11.glEnable(GL11.GL_CULL_FACE);
        highlightShader.unbind();
    }

    private void rebuildAllMeshes(World world) {
        for (ChunkMesh mesh : chunkMeshes) mesh.cleanup();
        chunkMeshes.clear();
        for (Chunk chunk : world.getChunks().values()) {
            chunkMeshes.add(new ChunkMesh(chunk, world));
        }
    }

    @Override
    public void cleanup() {
        if (shader != null) shader.cleanup();
        if (highlightShader != null) highlightShader.cleanup();
        for (ChunkMesh mesh : chunkMeshes) mesh.cleanup();
        GL15.glDeleteBuffers(highlightVbo);
        GL30.glDeleteVertexArrays(highlightVao);
    }
}
