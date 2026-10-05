#version 430 core

in vec3 vRel;
in float vViewDepth;
flat in vec4 vColor;

uniform vec4 uFogColor;
// Haze: x = density per block at sea level, y = scale height, z = sea level, w = camera y.
uniform vec4 uHaze;
// Fade-out towards the end of the LOD distance: start, end (horizontal blocks).
uniform vec2 uEdge;
// Planet curvature (see lod.vsh); y = 0 when flat.
uniform vec2 uBend;
// Vanilla draws nothing farther than this along the view direction.
uniform float uVanillaFar;

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

// Fraction of the light from this fragment scattered away by air whose density falls off
// exponentially with height (integrated exactly along the straight view ray).
float haze(vec3 rel) {
    float h = uHaze.y;
    float a = exp((uHaze.z - uHaze.w) / h);              // relative density at the camera
    float b = exp((uHaze.z - uHaze.w - rel.y) / h);      // ... and at the fragment
    float x = rel.y / h;
    float mean = abs(x) < 1e-3 ? a * (1.0 - 0.5 * x) : (a - b) / x;
    return 1.0 - exp(-uHaze.x * length(rel) * mean);
}

// True if the planet (a sea-level surface bent like the terrain) hides this point from the camera.
bool behindHorizon(vec3 rel) {
    float c = uBend.y;
    float cam = uHaze.w - uHaze.z;                    // camera height above sea level
    if (c <= 0.0 || cam <= 0.0) {
        return false;
    }
    float d2 = dot(rel.xz, rel.xz);
    float drop = cam - (uHaze.w + rel.y - uHaze.z);   // how far the point lies below the camera
    // The ray dips lowest relative to the surface at t = drop / (2 d^2 c); hidden if it goes under.
    return drop > 0.0 && drop < 2.0 * d2 * c && 4.0 * cam * d2 * c < drop * drop;
}

void main() {
#ifdef MASKED
    // Discard only well inside covered columns, so LODs overlap vanilla by a sliver and no
    // pixel cracks open along the seam (vanilla wins the overlap through the depth nudge).
    // Vanilla clips everything past its far plane, so LODs stay there even inside its area.
    vec3 local = vRel + uCamFrac;
    if (vViewDepth < uVanillaFar && covered(local.xz + vec2(SEAM, SEAM)) && covered(local.xz + vec2(-SEAM, SEAM))
            && covered(local.xz + vec2(SEAM, -SEAM)) && covered(local.xz + vec2(-SEAM, -SEAM))) {
        discard;
    }
#endif
    if (behindHorizon(vRel)) {
        discard;
    }
    float edge = smoothstep(uEdge.x, uEdge.y, length(vRel.xz));
    float fog = max(haze(vRel), edge);
    fragColor = vec4(mix(vColor.rgb, uFogColor.rgb, fog), vColor.a);
}
