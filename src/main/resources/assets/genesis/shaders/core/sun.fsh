#version 150

in vec3 v_camera_pos;
in vec3 v_entry_position;
in float v_half_size;

uniform vec3 Color0;
uniform vec3 Color1;
uniform float Opacity;
uniform float Time;
uniform float SpaceView;

out vec4 frag_color;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float valueNoise(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, local.x);
    float x10 = mix(n010, n110, local.x);
    float x01 = mix(n001, n101, local.x);
    float x11 = mix(n011, n111, local.x);
    return mix(mix(x00, x10, local.y), mix(x01, x11, local.y), local.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.52;
    for (int octave = 0; octave < 5; octave++) {
        value += valueNoise(p) * amplitude;
        p = p * 2.03 + vec3(17.1, 9.2, 13.7);
        amplitude *= 0.5;
    }
    return value;
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float valueNoise2(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash22(cell).x;
    float b = hash22(cell + vec2(1.0, 0.0)).x;
    float c = hash22(cell + vec2(0.0, 1.0)).x;
    float d = hash22(cell + vec2(1.0, 1.0)).x;
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm2(vec2 p) {
    float value = 0.0;
    float amplitude = 0.53;
    for (int octave = 0; octave < 6; octave++) {
        value += valueNoise2(p) * amplitude;
        p = mat2(1.67, -1.13, 1.13, 1.67) * p + vec2(7.7, 13.1);
        amplitude *= 0.49;
    }
    return value;
}

float worley(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    float nearest = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = hash22(cell + neighbor);
            vec2 delta = neighbor + point - local;
            nearest = min(nearest, dot(delta, delta));
        }
    }
    return sqrt(nearest);
}

vec2 cubeFaceUv(vec3 p) {
    vec3 a = abs(p);
    if (a.x >= a.y && a.x >= a.z) {
        return vec2(p.z * sign(p.x), p.y);
    }
    if (a.y >= a.z) {
        return vec2(p.x, p.z * sign(p.y));
    }
    return vec2(p.x * sign(p.z), p.y);
}

vec3 cubeNormal(vec3 p) {
    vec3 a = abs(p);
    if (a.x >= a.y && a.x >= a.z) {
        return vec3(sign(p.x), 0.0, 0.0);
    }
    if (a.y >= a.z) {
        return vec3(0.0, sign(p.y), 0.0);
    }
    return vec3(0.0, 0.0, sign(p.z));
}

void main() {
    vec3 local = v_entry_position / max(v_half_size, 0.001);
    vec3 normal = cubeNormal(local);
    vec3 viewDirection = normalize(v_camera_pos - v_entry_position);
    float rim = pow(1.0 - clamp(dot(normal, viewDirection), 0.0, 1.0), 1.65);
    vec3 color;

    if (SpaceView > 0.5) {
        vec2 uv = cubeFaceUv(local);
        vec2 drift = vec2(Time * 0.020, -Time * 0.014);
        float warpX = fbm2(uv * 3.1 + drift);
        float warpY = fbm2(uv * 3.1 - drift + vec2(19.4, 7.8));
        vec2 warped = uv * 15.5 + vec2(warpX, warpY) * 2.8 + drift * 1.8;

        float cells = 1.0 - smoothstep(0.08, 0.78, worley(warped));
        float micro = fbm2(uv * 45.0 - drift * 4.0);
        float filaments = fbm2(uv * 9.0 + vec2(warpY, -warpX) * 2.2 + drift);
        float broadFlow = fbm(local * 3.4 + vec3(Time * 0.010, -Time * 0.007, Time * 0.008));
        float activeRegion = smoothstep(0.66, 0.88, broadFlow + filaments * 0.18);
        float heat = clamp(0.16 + cells * 0.66 + micro * 0.23, 0.0, 1.0);

        vec3 deepOrange = mix(vec3(0.58, 0.075, 0.004), Color0, 0.60);
        vec3 hotGold = mix(Color0, Color1, 0.62);
        vec3 whiteHot = mix(Color1, vec3(1.0, 0.985, 0.82), 0.72);
        color = mix(deepOrange, hotGold, smoothstep(0.12, 0.60, heat));
        color = mix(color, whiteHot, smoothstep(0.48, 0.96, heat));
        color += vec3(1.0, 0.54, 0.06) * activeRegion * (0.11 + filaments * 0.10);
        color += vec3(1.0, 0.84, 0.36) * rim * 0.13;
        color *= 0.985 + 0.018 * sin(Time * 1.9 + broadFlow * 21.0);
    } else {
        // Through an atmosphere the tiny solar surface cannot resolve into
        // granules. Keep it clean and flat, then let the corona carry the glow.
        vec3 flatSun = mix(Color1, vec3(1.0, 0.985, 0.88), 0.84);
        color = flatSun * (1.18 + 0.010 * sin(Time * 1.35));
        color += vec3(1.0, 0.78, 0.30) * rim * 0.10;
    }

    frag_color = vec4(color, Opacity);
}
