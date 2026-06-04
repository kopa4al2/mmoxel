package com.stefan.voxel.client.render;

import org.lwjgl.opengl.GL20;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class Shader {
    private int programId;
    private final String vertexPath;
    private final String fragmentPath;
    private long lastModifiedVertex;
    private long lastModifiedFragment;

    public Shader(String vertexPath, String fragmentPath) {
        this.vertexPath = vertexPath;
        this.fragmentPath = fragmentPath;
        createProgram();
    }

    private void createProgram() {
        String vertexCode = loadSource(vertexPath);
        String fragmentCode = loadSource(fragmentPath);

        int vShader = compileShader(vertexCode, GL20.GL_VERTEX_SHADER);
        int fShader = compileShader(fragmentCode, GL20.GL_FRAGMENT_SHADER);

        int newProgramId = GL20.glCreateProgram();
        GL20.glAttachShader(newProgramId, vShader);
        GL20.glAttachShader(newProgramId, fShader);
        GL20.glLinkProgram(newProgramId);

        if (GL20.glGetProgrami(newProgramId, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(newProgramId);
            GL20.glDeleteShader(vShader);
            GL20.glDeleteShader(fShader);
            GL20.glDeleteProgram(newProgramId);
            throw new RuntimeException("Error linking shader program: " + log);
        }

        GL20.glDetachShader(newProgramId, vShader);
        GL20.glDetachShader(newProgramId, fShader);
        GL20.glDeleteShader(vShader);
        GL20.glDeleteShader(fShader);

        if (this.programId != 0) {
            GL20.glDeleteProgram(this.programId);
        }
        this.programId = newProgramId;

        this.lastModifiedVertex = getLastModified(vertexPath);
        this.lastModifiedFragment = getLastModified(fragmentPath);
    }

    private String loadSource(String pathStr) {
        try {
            // Try loading from file system first (for hot reload during development)
            Path path = Paths.get("client/src/main/resources", pathStr);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
            
            // Fallback to classpath resources
            var is = getClass().getResourceAsStream(pathStr.startsWith("/") ? pathStr : "/" + pathStr);
            if (is == null) {
                throw new RuntimeException("Shader not found: " + pathStr);
            }
            try (java.util.Scanner scanner = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8)) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader source: " + pathStr, e);
        }
    }

    private long getLastModified(String pathStr) {
        try {
            Path path = Paths.get("client/src/main/resources", pathStr);
            if (Files.exists(path)) {
                return Files.getLastModifiedTime(path).toMillis();
            }
        } catch (IOException ignored) {}
        return 0;
    }

    public void checkHotReload() {
        long currentVertex = getLastModified(vertexPath);
        long currentFragment = getLastModified(fragmentPath);

        if (currentVertex > lastModifiedVertex || currentFragment > lastModifiedFragment) {
            try {
                System.out.println("Reloading shader: " + vertexPath + ", " + fragmentPath);
                createProgram();
            } catch (Exception e) {
                System.err.println("Failed to hot reload shader: " + e.getMessage());
                // Keep the old program if reload fails
                lastModifiedVertex = currentVertex;
                lastModifiedFragment = currentFragment;
            }
        }
    }

    private int compileShader(String code, int type) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, code);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shaderId);
            GL20.glDeleteShader(shaderId);
            throw new RuntimeException("Error compiling shader (" + (type == GL20.GL_VERTEX_SHADER ? "VERTEX" : "FRAGMENT") + "): " + log);
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
