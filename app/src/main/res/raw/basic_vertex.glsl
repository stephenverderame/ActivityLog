#version 300 es
layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;
layout(location = 2) in vec3 normal;
uniform mat4 viewProj;
uniform mat4 model;
uniform mat4 normalMatrix;
uniform mat4 lightSpaceMatrix;
out vec2 tCoords;
out vec3 norm;
out vec3 fragPos;
out vec4 fragPosLightSpace;
void main(){
    gl_Position = viewProj * model * vec4(pos, 1.0);
    fragPos = vec3(model * vec4(pos, 1.0));
    tCoords = texCoords;
    norm = mat3(normalMatrix) * normal;
    fragPosLightSpace = lightSpaceMatrix * model * vec4(pos, 1.0);
}
