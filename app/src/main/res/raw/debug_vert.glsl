#version 300 es
layout(location = 0) in vec3 pos;
uniform mat4 viewProj;
uniform mat4 model;
void main(){
    gl_Position = viewProj * model * vec4(pos, 1.0);
}
