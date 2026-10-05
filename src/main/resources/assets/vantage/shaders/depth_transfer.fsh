#version 430 core

// Re-projects LOD depth into vanilla's depth range so clouds, particles and translucent
// geometry drawn later are hidden behind distant terrain correctly.
uniform sampler2D uDepth;
uniform mat4 uInvLodProj;
uniform mat4 uVanillaProj;
uniform int uReverseZ;

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    float d = texelFetch(uDepth, px, 0).r;
    if (uReverseZ == 1 ? d <= 0.0 : d >= 1.0) {
        discard;
    }
    vec2 ndc = gl_FragCoord.xy / vec2(textureSize(uDepth, 0)) * 2.0 - 1.0;
    float z = uReverseZ == 1 ? d : d * 2.0 - 1.0;
    vec4 view = uInvLodProj * vec4(ndc, z, 1.0);
    view /= view.w;
    // Nudge LODs slightly away so vanilla wins wherever both draw the same surface.
    view.xyz *= 1.003;
    vec4 clip = uVanillaProj * view;
    gl_FragDepth = clamp(clip.z / clip.w * 0.5 + 0.5, 0.0, 0.99999994);
}
