package com.stefan.voxel.client.render;

import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.Chunk;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {
    private int vaoId;
    private int vboId;
    private int iboId;
    private int vertexCount;

    private final Chunk chunk;

    public ChunkMesh(Chunk chunk) {
        this.chunk = chunk;
        generateMesh(chunk);
    }

    public Chunk getChunk() {
        return chunk;
    }

    private void generateMesh(Chunk chunk) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int indexOffset = 0;
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    if (type == BlockType.AIR) continue;

                    float[] color = getColor(type);
                    
                    // Simple cube mesh generation (all 6 faces)
                    // In a real voxel engine, we'd check adjacent blocks (culling)
                    addCube(x, y, z, color, vertices, indices, indexOffset);
                    indexOffset += 8;
                }
            }
        }

        vertexCount = indices.size();
        
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.size());
        for (Float v : vertices) vertexBuffer.put(v);
        vertexBuffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4);
        GL20.glEnableVertexAttribArray(1);

        iboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboId);
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.size());
        for (Integer i : indices) indexBuffer.put(i);
        indexBuffer.flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);

        GL30.glBindVertexArray(0);
    }

    private float[] getColor(BlockType type) {
        return switch (type) {
            case GRASS -> new float[]{0.1f, 0.8f, 0.1f};
            case DIRT -> new float[]{0.5f, 0.3f, 0.1f};
            case STONE -> new float[]{0.5f, 0.5f, 0.5f};
            default -> new float[]{1f, 1f, 1f};
        };
    }

    private void addCube(float x, float y, float z, float[] c, List<Float> v, List<Integer> ind, int offset) {
        // Positions and Colors interleaved: x, y, z, r, g, b
        float[] verts = {
            x, y, z, c[0], c[1], c[2],
            x+1, y, z, c[0], c[1], c[2],
            x+1, y+1, z, c[0], c[1], c[2],
            x, y+1, z, c[0], c[1], c[2],
            x, y, z+1, c[0], c[1], c[2],
            x+1, y, z+1, c[0], c[1], c[2],
            x+1, y+1, z+1, c[0], c[1], c[2],
            x, y+1, z+1, c[0], c[1], c[2],
        };
        for (float f : verts) v.add(f);

        int[] indices = {
            0, 1, 2, 2, 3, 0, // front
            1, 5, 6, 6, 2, 1, // right
            7, 6, 5, 5, 4, 7, // back
            4, 0, 3, 3, 7, 4, // left
            4, 5, 1, 1, 0, 4, // bottom
            3, 2, 6, 6, 7, 3  // top
        };
        for (int i : indices) ind.add(i + offset);
    }

    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(iboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
