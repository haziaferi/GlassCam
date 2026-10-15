#version 310 es
layout(rgba8,binding = 0) readonly uniform highp image2D inTexture;
layout(std430, binding = 1) buffer outWaveform {
    uint waveform[];
};
layout(location = 2) uniform int show_color;

// Original dispatch: width/64 × height/waveform_factor with local_size_x=64, local_size_y=1
layout (local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

const float factor = 8.0;
const int lookupstep = 2;
const float intensity = 0.01;
const float thres = 0.016;

void main() {
    vec2 storePos = vec2(gl_GlobalInvocationID.xy);
    vec2 imgsize = vec2(imageSize(inTexture).xy);

    // Precompute brightness band center and thresholds (outside loop)
    float s = storePos.y / (imgsize.y / factor);
    float maxb = s + thres;
    float minb = s - thres;

    vec3 col = vec3(0.0);
    int st = int(storePos.y);
    int size = int(imgsize.y);

    // Scan Y positions at fixed X within restricted band to reduce texture reads
    for (int y = st; y < size - st; y += lookupstep) {
        vec2 coords = vec2(storePos.x, float(y));
        vec3 texcol = imageLoad(inTexture, ivec2(coords)).rgb;

        if (show_color == 0) {
            // Color mode — add intensity per-channel when each RGB component falls within band
            col += vec3(intensity)*step(texcol,vec3(maxb))*step(vec3(minb),texcol);
        } else {
            // Average mode — check if average RGB falls within band
            float l = (texcol.r + texcol.g + texcol.b) / 3.0;
            col += vec3(intensity) * step(l - maxb, 0.0) * step(minb - l, 0.0);

            // White peaks turn red when fully accumulated
            if (col.r >= 0.99 && col.b >= 0.99 && col.g >= 0.99) {
                col = vec3(1.0, 0.0, 0.0);
            }
        }
    }

    // Storage formula matches original SSBO layout
    int pos = int(storePos.y * imgsize.x + (imgsize.x - storePos.x));
    ivec4 bytes = ivec4(col * 255.0, 255.0);
    uint integerValue = (uint(bytes.a) << 24) | (uint(bytes.r) << 16) | (uint(bytes.g) << 8) | uint(bytes.b);
    waveform[pos] = integerValue;
}
