#version 330 core
layout (location = 0) in vec4 vertex; // <vec2 pos, vec2 tex>
out vec2 TexCoords;

uniform vec2 translation;
uniform vec2 scale;

void main() {
    gl_Position = vec4(vertex.xy * scale + translation, 0.0, 1.0);
    TexCoords = vertex.zw;
}
