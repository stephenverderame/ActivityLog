#version 300 es
precision highp float;
in vec3 texCoord;
uniform samplerCube skyboxTexture;
out vec4 glFragColor;
void main(){
    glFragColor = texture(skyboxTexture, texCoord);
}
