#version 330 core
layout (location = 0) in vec2 position;

uniform vec2 translation;
uniform vec2 scale;

void main() {
    gl_Position = vec4(position * scale + translation, 0.0, 1.0);
}
