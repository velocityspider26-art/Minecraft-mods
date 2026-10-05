#version 430 core

in vec3 vRel;
flat in vec4 vColor;

uniform vec4 uFogColor;
uniform vec2 uFogRange;

#ifdef MASKED
// Chunks vanilla is drawing: LOD fragments there are discarded.
uniform usampler2D uCoverage;
uniform ivec4 uCoverageInfo; // origin chunk x, origin chunk z, size, coverage bit that hides this pass
uniform ivec3 uAnchor;
uniform vec3 uCamFrac;
#endif

out vec4 fragColor;

#ifdef MASKED
const float SEAM = 0.25;

bool covered(vec2 local) {
    ivec2 chunk = (uAnchor.xz + ivec2(floor(local))) >> 4;
    ivec2 cell = chunk - uCoverageInfo.xy;
    return all(greaterThanEqual(cell, ivec2(0))) && all(lessThan(cell, ivec2(uCoverageInfo.z)))
            && (texelFetch(uCoverage, cell, 0).r & uint(uCoverageInfo.w)) != 0u;
}
#endif

void main() {
#ifdef MASKED
    // Discard only well inside covered columns, so LODs overlap vanilla by a sliver and no
    // pixel cracks open along the seam (vanilla wins the overlap through the depth nudge).
    vec3 local = vRel + uCamFrac;
    if (covered(local.xz + vec2(SEAM, SEAM)) && covered(local.xz + vec2(-SEAM, SEAM))
            && covered(local.xz + vec2(SEAM, -SEAM)) && covered(local.xz + vec2(-SEAM, -SEAM))) {
        discard;
    }
#endif
    float dist = max(length(vRel.xz), abs(vRel.y));
    float fog = clamp((dist - uFogRange.x) / max(uFogRange.y - uFogRange.x, 1.0), 0.0, 1.0);
    fragColor = vec4(mix(vColor.rgb, uFogColor.rgb, fog), vColor.a);
}
