package com.stefan.voxel.client.render;

import com.stefan.voxel.core.world.BlockType;
import com.stefan.voxel.core.world.Chunk;
import com.stefan.voxel.core.world.World;
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

    // Face directions: +X, -X, +Y, -Y, +Z, -Z
    private static final int[][] FACE_NORMALS = {
        {1, 0, 0}, {-1, 0, 0},
        {0, 1, 0}, {0, -1, 0},
        {0, 0, 1}, {0, 0, -1}
    };

    // Vertices for each face (4 vertices per face), relative to block origin
    // Each face: 4 vertices with (dx, dy, dz)
    private static final float[][][] FACE_VERTICES = {
        // +X face
        {{1,0,0}, {1,1,0}, {1,1,1}, {1,0,1}},
        // -X face
        {{0,0,1}, {0,1,1}, {0,1,0}, {0,0,0}},
        // +Y face (top)
        {{0,1,0}, {0,1,1}, {1,1,1}, {1,1,0}},
        // -Y face (bottom)
        {{0,0,1}, {0,0,0}, {1,0,0}, {1,0,1}},
        // +Z face
        {{1,0,1}, {1,1,1}, {0,1,1}, {0,0,1}},
        // -Z face
        {{0,0,0}, {0,1,0}, {1,1,0}, {1,0,0}},
    };

    public ChunkMesh(Chunk chunk, World world) {
        this.chunk = chunk;
        generateMesh(chunk, world);
    }

    public Chunk getChunk() {
        return chunk;
    }

    private void generateMesh(Chunk chunk, World world) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int chunkWorldX = chunk.getPosition().x * Chunk.SIZE;
        int chunkWorldY = chunk.getPosition().y * Chunk.SIZE;
        int chunkWorldZ = chunk.getPosition().z * Chunk.SIZE;

        int indexOffset = 0;
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    BlockType type = chunk.getBlock(x, y, z);
                    if (type == BlockType.AIR) continue;

                    float[] color = getColor(type);
                    int worldX = chunkWorldX + x;
                    int worldY = chunkWorldY + y;
                    int worldZ = chunkWorldZ + z;

                    for (int face = 0; face < 6; face++) {
                        int nx = FACE_NORMALS[face][0];
                        int ny = FACE_NORMALS[face][1];
                        int nz = FACE_NORMALS[face][2];

                        // Check neighbor block - use world for cross-chunk boundaries
                        BlockType neighbor = world.getBlock(worldX + nx, worldY + ny, worldZ + nz);
                        if (neighbor != BlockType.AIR) continue;

                        // Add 4 vertices for this face
                        // Vertex format: x, y, z, nx, ny, nz, r, g, b (9 floats)
                        float[][] fv = FACE_VERTICES[face];
                        for (int v = 0; v < 4; v++) {
                            vertices.add(x + fv[v][0]);
                            vertices.add(y + fv[v][1]);
                            vertices.add(z + fv[v][2]);
                            vertices.add((float) nx);
                            vertices.add((float) ny);
                            vertices.add((float) nz);
                            vertices.add(color[0]);
                            vertices.add(color[1]);
                            vertices.add(color[2]);
                        }

                        // Two triangles per face
                        indices.add(indexOffset);
                        indices.add(indexOffset + 1);
                        indices.add(indexOffset + 2);
                        indices.add(indexOffset);
                        indices.add(indexOffset + 2);
                        indices.add(indexOffset + 3);
                        indexOffset += 4;
                    }
                }
            }
        }

        vertexCount = indices.size();
        if (vertexCount == 0) return;

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.size());
        for (Float v : vertices) vertexBuffer.put(v);
        vertexBuffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);

        int stride = 9 * 4; // 9 floats * 4 bytes
        // position
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(0);
        // normal
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 3 * 4);
        GL20.glEnableVertexAttribArray(1);
        // color
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 6 * 4);
        GL20.glEnableVertexAttribArray(2);

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
            case GRASS   -> new float[]{0.2f, 0.7f, 0.15f};
            case DIRT    -> new float[]{0.55f, 0.35f, 0.15f};
            case STONE   -> new float[]{0.5f, 0.5f, 0.5f};
            case SAND    -> new float[]{0.85f, 0.8f, 0.55f};
            case WATER   -> new float[]{0.2f, 0.4f, 0.8f};
            case BEDROCK -> new float[]{0.15f, 0.15f, 0.15f};
            case SNOW    -> new float[]{0.95f, 0.95f, 0.98f};
            case GRAVEL  -> new float[]{0.6f, 0.55f, 0.5f};
            case LOG     -> new float[]{0.4f, 0.25f, 0.1f};
            case LEAVES  -> new float[]{0.1f, 0.5f, 0.1f};
            default      -> new float[]{1f, 1f, 1f};
        };
    }

    public void render() {
        if (vertexCount == 0) return;
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        if (vertexCount == 0) return;
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(iboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
