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
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class OpenGLRenderer implements Renderer {
    private Shader shader;
    private final List<ChunkMesh> chunkMeshes = new ArrayList<>();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    private final String vertexShaderSource = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aColor;
        
        out vec3 ourColor;
        
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        
        void main() {
            gl_Position = projection * view * model * vec4(aPos, 1.0);
            ourColor = aColor;
        }
        """;

    private final String fragmentShaderSource = """
        #version 330 core
        out vec4 FragColor;
        in vec3 ourColor;
        
        void main() {
            FragColor = vec4(ourColor, 1.0);
        }
        """;

    @Override
    public void init() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        shader = new Shader(vertexShaderSource, fragmentShaderSource);
    }

    @Override
    public void render(World world, Matrix4f projection, Matrix4f view) {
        // Simple chunk loading logic for now
        if (chunkMeshes.size() != world.getChunks().size()) {
            for (ChunkMesh mesh : chunkMeshes) mesh.cleanup();
            chunkMeshes.clear();
            for (Chunk chunk : world.getChunks().values()) {
                chunkMeshes.add(new ChunkMesh(chunk));
            }
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
    }

    @Override
    public void cleanup() {
        if (shader != null) shader.cleanup();
        for (ChunkMesh mesh : chunkMeshes) mesh.cleanup();
    }
}
