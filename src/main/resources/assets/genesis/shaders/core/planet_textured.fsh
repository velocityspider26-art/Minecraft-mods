#version 150

in vec2 texCoord;
in vec4 vertexColor;
in vec3 vNormal;
in vec3 lightDir;

in vec3 v_entry_position;

uniform sampler2D Sampler0;
uniform vec3 SkyColor;
// 1.0 = pin to the far plane (sky furniture on a planet surface, so terrain
// always draws over it); 0.0 = write real depth (in space, so a nearer planet
// genuinely occludes the star behind it).
uniform float FlatDepth;

// Drifting cloud shell. CloudDensity is the body's atmosphere density, so an
// airless world passes 0 and the whole layer costs nothing and shows nothing.
uniform float CloudTime;
uniform float CloudDensity;

out vec4 frag_color;

// Value noise over the sphere's own direction vector, so the pattern is stable
// as the planet rotates instead of swimming across its surface.
float hash13(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash13(i + vec3(0, 0, 0)), hash13(i + vec3(1, 0, 0)), f.x),
            mix(hash13(i + vec3(0, 1, 0)), hash13(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(hash13(i + vec3(0, 0, 1)), hash13(i + vec3(1, 0, 1)), f.x),
            mix(hash13(i + vec3(0, 1, 1)), hash13(i + vec3(1, 1, 1)), f.x), f.y),
        f.z);
}

// Layered noise. Clouds are structure at several sizes at once; a single
// octave reads as fog rather than weather.
float cloudField(vec3 dir) {
    vec3 drift = vec3(CloudTime * 0.02, 0.0, 0.0);
    float total = 0.0;
    float amplitude = 0.5;
    float frequency = 3.0;
    for (int octave = 0; octave < 4; octave++) {
        total += valueNoise(dir * frequency + drift) * amplitude;
        frequency *= 2.1;
        amplitude *= 0.5;
        drift *= 1.7;
    }
    return total;
}

void main() {
    // Sample the planet texture
    vec4 texColor = texture(Sampler0, texCoord);

    // Simple directional lighting from the passed-in normal
    vec3 n = normalize(v_entry_position);
    float ndotl = max(dot(n, -normalize(lightDir)), 0.0);

    ndotl = 1. - 1. / (ndotl * ndotl * 5. + 1.);

    float ambient = 0.28;
    float lighting = clamp(ambient + ndotl, 0.0, 1.0);

    vec3 surfaceColor = texColor.rgb;

    // Clouds sit above the surface, so they are laid over the texture before
    // lighting is applied — that way they fall into night on the dark side
    // along with everything else, instead of glowing there.
    if (CloudDensity > 0.001) {
        float clouds = cloudField(n);
        // Threshold well above the noise floor so there are clear skies between
        // the cloud masses rather than an even overcast covering the planet.
        float coverage = smoothstep(0.52, 0.78, clouds) * CloudDensity;
        surfaceColor = mix(surfaceColor, vec3(1.0), coverage * 0.85);
    }

    vec3 litTexColor = surfaceColor * lighting;

    litTexColor += SkyColor * clamp(1 - lighting, 0, 1);

    // Interpolate between fog color and texture color based on alpha
    vec3 finalColor = mix(vertexColor.rgb, litTexColor, vertexColor.a);

    // Planets are always fully opaque
    frag_color = vec4(finalColor, 1.0);
    gl_FragDepth = FlatDepth > 0.5 ? 1.0 : gl_FragCoord.z;
}
