#version 300 es
precision highp float;
uniform sampler2D inputImageTexture;
precision highp sampler3D;
uniform sampler3D uLutTexture;
in vec2 textureCoordinate;
uniform float colorAdjustStrength;
out vec4 oColor;

void main() {
    vec3 color = texture(inputImageTexture, textureCoordinate).rgb;
    color = clamp(color, 0.0, 1.0); // Ensure the RGB components are between [0,1]
	
    // Apply color grading using a 3D lookup table (LUT) texture
    vec3 transformedColor = texture(uLutTexture, vec3(color.r, color.g, color.b)).rgb;
	transformedColor = mix(color, transformedColor, colorAdjustStrength); // 根据调节强度对颜色进行插值处理
	transformedColor = clamp(transformedColor, 0.0, 1.0);

    oColor = vec4(transformedColor, 1.0);

}