#version 300 es
in vec4 position;
in vec2 inputTextureCoordinate;

out vec2 textureCoordinate;

void main(void) {
   gl_Position = position;
   textureCoordinate = inputTextureCoordinate;
}