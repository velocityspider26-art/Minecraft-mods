#version 430 core

// One LOD quad is two uints, pulled by gl_VertexID (4 vertices per quad, shared index buffer).
// x: x | y<<5 | z<<10 | (w-1)<<15 | (h-1)<<20 | face<<25 | sky<<28
// y: vid | block<<20
layout(std430, binding = 0) readonly buffer Quads {
    uvec2 quads[];
};

// Per visual id: 6 face colours (RGBA8, Direction order), class, unused.
layout(std430, binding = 1) readonly buffer Visuals {
    uint visuals[];
};

// Per section: origin relative to the camera anchor block, and level | mask<<8.
layout(location = 0) in ivec4 aSection;

uniform mat4 uViewProj;
uniform vec3 uCamFrac;
uniform sampler2D uLightmap;

out vec3 vRel;
flat out vec4 vColor;

// Face order follows Minecraft's Direction: down, up, north, south, west, east.
// U x V points out of the face, so (0,1,2)(2,3,0) is counter-clockwise from outside.
const ivec3 NORMAL[6] = ivec3[6](ivec3(0, -1, 0), ivec3(0, 1, 0), ivec3(0, 0, -1), ivec3(0, 0, 1), ivec3(-1, 0, 0), ivec3(1, 0, 0));
const ivec3 U_AXIS[6] = ivec3[6](ivec3(1, 0, 0), ivec3(0, 0, 1), ivec3(0, 1, 0), ivec3(1, 0, 0), ivec3(0, 0, 1), ivec3(0, 1, 0));
const ivec3 V_AXIS[6] = ivec3[6](ivec3(0, 0, 1), ivec3(1, 0, 0), ivec3(1, 0, 0), ivec3(0, 1, 0), ivec3(0, 1, 0), ivec3(0, 0, 1));
const float SHADE[6] = float[6](0.5, 1.0, 0.8, 0.8, 0.6, 0.6);

void main() {
    uvec2 q = quads[gl_VertexID >> 2];
    int corner = gl_VertexID & 3;

    ivec3 p = ivec3(int(q.x & 31u), int((q.x >> 5) & 31u), int((q.x >> 10) & 31u));
    int w = int((q.x >> 15) & 31u) + 1;
    int h = int((q.x >> 20) & 31u) + 1;
    int face = int((q.x >> 25) & 7u);
    int sky = int(q.x >> 28);
    int vid = int(q.y & 0xFFFFFu);
    int block = int((q.y >> 20) & 15u);

    int cu = (corner == 1 || corner == 2) ? 1 : 0;
    int cv = corner >= 2 ? 1 : 0;
    p += max(NORMAL[face], ivec3(0)) + U_AXIS[face] * (cu * w) + V_AXIS[face] * (cv * h);

    int level = aSection.w & 0xFF;
    ivec3 rel = aSection.xyz + (p << level);
    vRel = vec3(rel) - uCamFrac;
    gl_Position = uViewProj * vec4(vRel, 1.0);

    vec4 color = unpackUnorm4x8(visuals[vid * 8 + face]);
#ifdef LEVEL_COLORS
    const vec3 LEVEL_TINT[7] = vec3[7](vec3(0.2, 0.9, 0.2), vec3(0.95, 0.9, 0.2), vec3(1.0, 0.55, 0.1), vec3(0.9, 0.15, 0.1),
            vec3(0.85, 0.2, 0.85), vec3(0.2, 0.35, 1.0), vec3(0.2, 0.9, 0.95));
    color.rgb = LEVEL_TINT[min(level, 6)];
#endif
    vec3 light = texelFetch(uLightmap, ivec2(block, sky), 0).rgb;
    vColor = vec4(color.rgb * light * SHADE[face], color.a);
}
