package com.stefan.voxel.client.render.opengl;

import com.stefan.voxel.client.render.Shader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class TextRenderer {
    private final Shader shader;
    private final int vaoId;
    private final int vboId;
    private int fontTextureId;
    
    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private int textureWidth;
    private int textureHeight;

    public record Glyph(int x, int y, int width, int height, int advance) {}

    public TextRenderer() {
        this.shader = new Shader("shaders/opengl/text/text.vert", "shaders/opengl/text/text.frag");
        
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        // We will update the VBO content for each character
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) 6 * 4 * 4, GL15.GL_DYNAMIC_DRAW);
        
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 4 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        
        GL30.glBindVertexArray(0);

        createFontTexture(new Font("Arial", Font.PLAIN, 64));
    }

    private void createFontTexture(Font font) {
        // Simple bitmap font generation using Java AWT
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        
        int maxHeight = fm.getHeight();
        int totalWidth = 0;
        for (int i = 32; i < 127; i++) {
            totalWidth += fm.charWidth(i) + 2;
        }
        
        textureWidth = totalWidth;
        textureHeight = maxHeight;
        
        BufferedImage fontSheet = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        g2d = fontSheet.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        
        int x = 0;
        for (int i = 32; i < 127; i++) {
            char c = (char) i;
            int w = fm.charWidth(c);
            int h = fm.getHeight();
            g2d.drawString(String.valueOf(c), x, fm.getAscent());
            glyphs.put(c, new Glyph(x, 0, w, h, w));
            x += w + 2;
        }
        g2d.dispose();

        // Convert BufferedImage to ByteBuffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(textureWidth * textureHeight);
        for (int y = 0; y < textureHeight; y++) {
            for (int xPos = 0; xPos < textureWidth; xPos++) {
                int argb = fontSheet.getRGB(xPos, y);
                // We only need the alpha channel or just a single channel
                byte alpha = (byte) ((argb >> 24) & 0xFF);
                buffer.put(alpha);
            }
        }
        buffer.flip();

        fontTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, textureWidth, textureHeight, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, buffer);
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    public void drawText(String text, float x, float y, float scale, float r, float g, float b) {
        shader.checkHotReload();
        shader.bind();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL20.glUniform3f(shader.getUniformLocation("textColor"), r, g, b);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
        
        GL30.glBindVertexArray(vaoId);

        float currentX = x;
        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph == null) continue;

            float xpos = currentX;
            float ypos = y; // Simplified, assuming baseline y
            float w = glyph.width * scale;
            float h = glyph.height * scale;

            float u0 = (float) glyph.x / textureWidth;
            float v0 = (float) glyph.y / textureHeight;
            float u1 = (float) (glyph.x + glyph.width) / textureWidth;
            float v1 = (float) (glyph.y + glyph.height) / textureHeight;

            float[] vertices = {
                xpos,     ypos + h,   u0, v0,
                xpos,     ypos,       u0, v1,
                xpos + w, ypos,       u1, v1,

                xpos,     ypos + h,   u0, v0,
                xpos + w, ypos,       u1, v1,
                xpos + w, ypos + h,   u1, v0
            };

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, vertices);
            
            GL20.glUniform2f(shader.getUniformLocation("translation"), 0, 0);
            GL20.glUniform2f(shader.getUniformLocation("scale"), 1, 1);

            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            
            currentX += glyph.advance * scale;
        }

        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public float getTextWidth(String text, float scale) {
        float width = 0;
        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph != null) {
                width += glyph.advance * scale;
            }
        }
        return width;
    }

    public float getTextHeight(String text, float scale) {
        float maxHeight = 0;
        for (char c : text.toCharArray()) {
            Glyph glyph = glyphs.get(c);
            if (glyph != null) {
                maxHeight = Math.max(maxHeight, glyph.height * scale);
            }
        }
        return maxHeight;
    }

    public void cleanup() {
        shader.cleanup();
        GL11.glDeleteTextures(fontTextureId);
        GL15.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
