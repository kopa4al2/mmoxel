package com.stefan.voxel.client.render;

import org.lwjgl.opengl.GL20;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Shader {
    private final int programId;

    public Shader(String vertexCode, String fragmentCode) {
        programId = GL20.glCreateProgram();
        int vShader = compileShader(vertexCode, GL20.GL_VERTEX_SHADER);
        int fShader = compileShader(fragmentCode, GL20.GL_FRAGMENT_SHADER);
        
        GL20.glAttachShader(programId, vShader);
        GL20.glAttachShader(programId, fShader);
        GL20.glLinkProgram(programId);
        
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader program: " + GL20.glGetProgramInfoLog(programId));
        }
        
        GL20.glDetachShader(programId, vShader);
        GL20.glDetachShader(programId, fShader);
        GL20.glDeleteShader(vShader);
        GL20.glDeleteShader(fShader);
    }

    private int compileShader(String code, int type) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, code);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Error compiling shader: " + GL20.glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(programId, name);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }
}
