#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 direction;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // The skybox cube is centered on the camera, so the vertex position IS the
    // world-space view direction for this fragment.
    direction = Position;
}
