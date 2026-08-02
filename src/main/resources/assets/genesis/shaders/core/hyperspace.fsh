#version 150

// Procedural hyperspace tunnel, modeled on the classic cockpit view:
// swirling blue/purple luminous clouds streaming past, bright cyan rays
// radiating from a white-hot core directly ahead, deep indigo behind.

uniform float GameTime;
uniform vec4 ColorModulator;
uniform vec3 TunnelAxis;

in vec3 direction;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * vnoise(p);
        p = p * 2.13 + vec2(31.7, 17.3);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec3 dir = normalize(direction);
    vec3 axis = normalize(TunnelAxis);

    // GameTime wraps every 24000 ticks; scale back up to ticks for animation.
    float t = GameTime * 24000.0;

    float forward = dot(dir, axis);

    // Basis perpendicular to the tunnel axis.
    vec3 ref = abs(axis.y) > 0.99 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
    vec3 u = normalize(cross(ref, axis));
    vec3 v = cross(axis, u);
    float angle = atan(dot(dir, v), dot(dir, u));

    // Cylindrical depth: huge near the core ahead, so features converge and
    // accelerate toward the center exactly like a perspective tunnel.
    float perp = max(length(dir - axis * forward), 0.06);
    float depth = clamp(forward / perp, -8.0, 12.0);

    // --- Swirling cloud layers (two, counter-rotating) ---
    vec2 cloudUvA = vec2(angle * 2.2 + t * 0.010, depth * 0.9 - t * 0.055);
    vec2 cloudUvB = vec2(angle * 3.1 - t * 0.014, depth * 1.4 - t * 0.085);
    float cloudA = fbm(cloudUvA);
    float cloudB = fbm(cloudUvB + cloudA * 1.7);
    float clouds = cloudA * 0.6 + cloudB * 0.7;

    // --- Radial light rays streaming from the core ---
    float rayNoise = vnoise(vec2(angle * 9.0, t * 0.02))
                   + 0.5 * vnoise(vec2(angle * 23.0 + 7.0, depth * 0.35 - t * 0.11));
    float rays = pow(clamp(rayNoise - 0.55, 0.0, 1.0) * 2.4, 2.4);

    // --- Compose colors (video palette: indigo -> blue -> cyan -> white) ---
    vec3 deepIndigo = vec3(0.030, 0.030, 0.140);
    vec3 midBlue    = vec3(0.100, 0.160, 0.620);
    vec3 violet     = vec3(0.270, 0.160, 0.620);
    vec3 cyan       = vec3(0.480, 0.760, 1.000);

    vec3 color = deepIndigo;
    color = mix(color, midBlue, clamp(clouds * 0.95, 0.0, 1.0));
    color = mix(color, violet, clamp(cloudB - cloudA * 0.5, 0.0, 1.0) * 0.55);

    // Rays get stronger toward the front half of the tunnel.
    float front = smoothstep(-0.2, 0.9, forward);
    color += cyan * rays * (0.25 + 0.75 * front);

    // White-hot core straight ahead, with a wide soft glow around it.
    float core = pow(max(forward, 0.0), 7.0);
    float hotCore = pow(max(forward, 0.0), 42.0);
    color += vec3(0.55, 0.70, 1.00) * core * 1.15;
    color += vec3(1.00, 1.00, 1.00) * hotCore * 2.2;

    // Slight pulse so the tunnel feels alive.
    color *= 0.92 + 0.08 * sin(t * 0.11 + clouds * 4.0);

    // Behind the ship the tunnel recedes into darkness.
    color *= 0.35 + 0.65 * smoothstep(-1.0, 0.55, forward);

    fragColor = vec4(color, 1.0) * ColorModulator;
}
