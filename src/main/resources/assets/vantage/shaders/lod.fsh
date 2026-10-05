#version 430 core

in vec3 vRel;
flat in vec4 vColor;

uniform vec4 uFogColor;
uniform vec2 uFogRange;

#ifdef MASKED
// Chunks vanilla is drawing: LOD fragments there are discarded.
uniform usampler2D uCoverage;
uniform ivec4 uCoverageInfo; // origin chunk x, origin chunk z, size, unused
uniform ivec3 uAnchor;
uniform vec3 uCamFrac;
#endif

out vec4 fragColor;

void main() {
#ifdef MASKED
    vec3 local = vRel + uCamFrac;
    ivec2 chunk = (uAnchor.xz + ivec2(floor(local.xz))) >> 4;
    ivec2 cell = chunk - uCoverageInfo.xy;
    if (all(greaterThanEqual(cell, ivec2(0))) && all(lessThan(cell, ivec2(uCoverageInfo.z)))
            && texelFetch(uCoverage, cell, 0).r != 0u) {
        discard;
    }
#endif
    float dist = max(length(vRel.xz), abs(vRel.y));
    float fog = clamp((dist - uFogRange.x) / max(uFogRange.y - uFogRange.x, 1.0), 0.0, 1.0);
    fragColor = vec4(mix(vColor.rgb, uFogColor.rgb, fog), vColor.a);
}
