package com.stefan.voxel.client.render.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class GuiRenderer {
    private final int vaoId;
    private final int vboId;
    private final GuiShader shader;

    public GuiRenderer() {
        this.shader = new GuiShader("shaders/opengl/ui/gui.vert", "shaders/opengl/ui/gui.frag");

        float[] vertices = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
             1.0f,  1.0f,
             1.0f, -1.0f
        };

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(0);
    }

    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        shader.checkHotReload();
        shader.bind();
        
        GL20.glUniform2f(shader.getUniformLocation("translation"), x, y);
        GL20.glUniform2f(shader.getUniformLocation("scale"), width, height);
        GL20.glUniform4f(shader.getUniformLocation("color"), r, g, b, a);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
        
        shader.unbind();
    }

    public void cleanup() {
        shader.cleanup();
        GL15.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
    }

    private static class GuiShader extends com.stefan.voxel.client.render.Shader {
        public GuiShader(String vertexCode, String fragmentCode) {
            super(vertexCode, fragmentCode);
        }
    }
}
