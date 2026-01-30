#version 150

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// 1.21.6: Custom uniforms must be in uniform blocks
layout(std140) uniform BlueprintConfig {
    float LumaRamp;
    float LumaLevel;
};

void main(){
    vec4 center = texture(InSampler, texCoord);
    vec4 gray = vec4(0.3, 0.59, 0.11, 0.0);

    // Get luminance of center pixel and adjust
    float centerLuma = dot(center + (center - pow(center, vec4(LumaRamp))), gray);

    // Quantize the luma value
    centerLuma = centerLuma - fract(centerLuma * LumaLevel) / LumaLevel;

    // Re-scale to full range
    centerLuma = centerLuma * (LumaLevel / (LumaLevel - 1.0));
    centerLuma = centerLuma * 0.5 + 0.25;

    fragColor = vec4(1.0 - centerLuma, 1.0 - centerLuma, clamp(1.5 - centerLuma, 0.0, 1.0), 1.0);
}
