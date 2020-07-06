#version 300 es
layout(location = 0) in vec3 pos;
out vec3 texCoord;
uniform mat4 view;
uniform mat4 proj;
uniform mat4 model;
void main(){
    texCoord = pos;
    gl_Position = proj * mat4(mat3(view)) * model * vec4(pos, 1.0);
}
