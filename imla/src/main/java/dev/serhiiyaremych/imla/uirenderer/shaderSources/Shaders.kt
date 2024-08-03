package dev.serhiiyaremych.imla.uirenderer.shaderSources

public const val DEFAULT_QUAD_VERT: String = """#version 300 es
precision mediump float;

uniform mat4 u_ViewProjection;
layout (location = 0) in vec2 a_TexCoord;
layout (location = 1) in vec4 a_Position;
layout (location = 2) in float a_TexIndex;
layout (location = 3) in float a_FlipTexture;
layout (location = 4) in float a_IsExternalTexture;
layout (location = 5) in float a_Alpha;
layout (location = 6) in float a_Mask;
layout (location = 7) in vec2 a_MaskCoord;

struct VertexOutput
{
    float texIndex;
    float flipTexture;
    float isExternalTexture;
    float alpha;
    float mask;
};

out vec2 maskCoord;
out vec2 texCoord;
out VertexOutput data;

void main() {
    maskCoord = a_MaskCoord;
    texCoord = a_TexCoord;
    data.texIndex = a_TexIndex;
    data.flipTexture = a_FlipTexture;
    data.isExternalTexture = a_IsExternalTexture;
    data.alpha = a_Alpha;
    data.mask = a_Mask;
    gl_Position = u_ViewProjection * a_Position;
}"""

public const val DEFAULT_QUAD_FRAG: String = """#version 300 es
#extension GL_OES_standard_derivatives: enable
precision mediump float;

struct VertexOutput
{
    float texIndex;
    float flipTexture;
    float isExternalTexture;
    float alpha;
    float mask;
};

uniform sampler2D u_Textures[8]; // todo: pre-process source before compilation to set HW value

in vec2 maskCoord;
in vec2 texCoord;
in VertexOutput data;

out vec4 color;

void main()
{
    vec4 baseColor = vec4(1.);
    vec2 texCoord = mix(texCoord, vec2(texCoord.x, 1.0 - texCoord.y), data.flipTexture);

    switch (int(data.texIndex)) {
        case 0:
            baseColor = texture(u_Textures[0], texCoord);break;
        case 1:
            baseColor = texture(u_Textures[1], texCoord);break;
        case 2:
            baseColor = texture(u_Textures[2], texCoord);break;
        case 3:
            baseColor = texture(u_Textures[3], texCoord);break;
        case 4:
            baseColor = texture(u_Textures[4], texCoord);break;
        case 5:
            baseColor = texture(u_Textures[5], texCoord);break;
        case 6:
            baseColor = texture(u_Textures[6], texCoord);break;
        case 7:
            baseColor = texture(u_Textures[7], texCoord);break;
    }

    baseColor.a = data.alpha;
    color = baseColor;
}"""

public const val EXTERNAL_QUAD_FRAG: String = """#version 300 es
#extension GL_OES_EGL_image_external_essl3: enable
#extension GL_OES_EGL_image_external: require
precision mediump float;

struct VertexOutput
{
    float texIndex;
    float flipTexture;
    float isExternalTexture;
    float alpha;
    float mask;
};

uniform samplerExternalOES u_Textures[8];

in vec2 maskCoord;
in vec2 texCoord;
in VertexOutput data;

out vec4 color;

void main()
{
    vec4 baseColor = vec4(1.);
    vec2 texCoord = mix(texCoord, vec2(texCoord.x, 1.0 - texCoord.y), data.flipTexture);

    switch (int(data.texIndex)) {
        case 0:
            baseColor = texture(u_Textures[0], texCoord);break;
        case 1:
            baseColor = texture(u_Textures[1], texCoord);break;
        case 2:
            baseColor = texture(u_Textures[2], texCoord);break;
        case 3:
            baseColor = texture(u_Textures[3], texCoord);break;
        case 4:
            baseColor = texture(u_Textures[4], texCoord);break;
        case 5:
            baseColor = texture(u_Textures[5], texCoord);break;
        case 6:
            baseColor = texture(u_Textures[6], texCoord);break;
        case 7:
            baseColor = texture(u_Textures[7], texCoord);break;
    }

    baseColor.a = data.alpha;
    color = baseColor;
}"""

public const val SIMPLE_QUAD_VERT: String = """#version 300 es
precision mediump float;

layout (std140) uniform TextureDataUBO {
    vec2 uv[4];         // x, y,
    //    vec2 size;          // width, height
    float flipTexture;  // flip Y texture coordinate
    float alpha;        // alpha blending of the texture
} textureData;

layout (location = 0) in vec2 aPosition;

out vec2 maskCoord;
out vec2 texCoord;
//out vec2 texSize;
out float alpha;
out float flip;

void main() {
    vec2 ndcPos;

    // Set the final position of the vertex in clip space
    gl_Position = vec4(aPosition, 0.0, 1.0);

    alpha = textureData.alpha;

    maskCoord = aPosition * 0.5 + 0.5;
    maskCoord.y = abs(textureData.flipTexture - maskCoord.y);
    texCoord = textureData.uv[gl_VertexID % 4];
    texCoord.y = abs(textureData.flipTexture - texCoord.y);
    flip = textureData.flipTexture;
    //    texSize = textureData.size;
}"""

public const val SIMPLE_QUAD_EXT_FRAG: String = """#version 300 es
#extension GL_OES_EGL_image_external_essl3: enable
#extension GL_OES_EGL_image_external: require
precision mediump float;

uniform samplerExternalOES u_Texture;

in vec2 maskCoord;
in vec2 texCoord;
in vec2 texSize;
in float alpha;
in float flip;

out vec4 color;

void main()
{
    vec4 baseColor = texture(u_Texture, texCoord);
    baseColor.a = alpha;
    color = baseColor;
}"""

public const val SIMPLE_QUAD_FRAG: String = """#version 300 es
#extension GL_OES_standard_derivatives: enable
precision mediump float;

uniform sampler2D u_Texture;

in vec2 maskCoord;
in vec2 texCoord;
in float alpha;

out vec4 color;

void main()
{
    vec4 baseColor = texture(u_Texture, texCoord);
    baseColor.a = alpha;
    color = baseColor;
}
"""

public const val BLUR_QUAD_FRAG: String = """#version 300 es
precision mediump float;

#define GAMMA 2.2

struct VertexOutput
{
    float texIndex;
    float flipTexture;
    float isExternalTexture;
    float alpha;
    float mask;
};
// horiz=(1.0, 0.0), vert=(0.0, 1.0)
uniform vec2 u_BlurDirection;
uniform vec2 u_TexelSize;
uniform float u_BlurSigma;
uniform vec4 u_BlurTint;

uniform sampler2D u_Textures[8];

in vec2 maskCoord;
in vec2 texCoord;
in VertexOutput data;

out vec4 color;


//Classic gamma correction functions
vec3 linear_from_srgb(vec3 srgb) {
    return vec3(
        srgb.r <= 0.04045 ? srgb.r / 12.92 : pow((srgb.r + 0.055) / 1.055, GAMMA),
        srgb.g <= 0.04045 ? srgb.g / 12.92 : pow((srgb.g + 0.055) / 1.055, GAMMA),
        srgb.b <= 0.04045 ? srgb.b / 12.92 : pow((srgb.b + 0.055) / 1.055, GAMMA)
    );
}

vec3 srgb_from_linear(vec3 lin) {
    return vec3(
        lin.r <= 0.0031308 ? lin.r * 12.92 : 1.055 * pow(lin.r, 1.0 / GAMMA) - 0.055,
        lin.g <= 0.0031308 ? lin.g * 12.92 : 1.055 * pow(lin.g, 1.0 / GAMMA) - 0.055,
        lin.b <= 0.0031308 ? lin.b * 12.92 : 1.055 * pow(lin.b, 1.0 / GAMMA) - 0.055
    );
}

float gaussianWeight(float x, float sigma) {
    return exp(-0.5 * (x * x) / (sigma * sigma));
}

void main() {
    bool flipTexture = int(data.flipTexture) > 0;
    vec2 loc = mix(texCoord, vec2(texCoord.x, 1.0 - texCoord.y), data.flipTexture);
    vec2 dir = u_BlurDirection / u_TexelSize;
    vec4 acc = vec4(0.0);
    float totalWeight = 0.0;
    int support = int(u_BlurSigma) * 3;
    float sigma = u_BlurSigma;
    //    int step = 9; // creates an interesting effect of embossed glass
    int step = 1;

    for (int i = -support; i <= support; i += step) {
        float fi = float(i);
        float x = fi;
        float weight = gaussianWeight(x, sigma);
        vec4 texColor = texture(u_Textures[1], loc + x * dir); // todo: support batching
        texColor.rgb = linear_from_srgb(texColor.rgb);
        acc += texColor * weight;
        totalWeight += weight;
    }
    acc.rgb = srgb_from_linear(acc.rgb * (1.0 / totalWeight));
    acc.a *= (1.0 / totalWeight);
    color = mix(acc, u_BlurTint, u_BlurTint.a * u_BlurTint.a);
}"""