#version 330 core
in vec3 fragColor;
in vec3 fragNormal;
in vec3 fragWorldPos;

out vec4 FragColor;

void main() {
    // Simple directional light from upper-right
    vec3 lightDir = normalize(vec3(0.3, 1.0, 0.5));
    float ambient = 0.4;
    float diffuse = max(dot(fragNormal, lightDir), 0.0) * 0.6;
    float light = ambient + diffuse;

    FragColor = vec4(fragColor * light, 1.0);
}
