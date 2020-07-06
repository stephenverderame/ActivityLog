#version 300 es
precision mediump float;
in vec2 tCoords;
in vec3 norm;
in vec3 fragPos;
in vec4 fragPosLightSpace;
out vec4 glFragColor;
struct dirLight{
    vec3 position;
    vec3 color;
    float ambientFac, specFac, diffFac;
};
uniform sampler2D tex;
uniform sampler2D shadowMap;
uniform vec4 color;
uniform bool useColor;
uniform dirLight sun;
uniform vec3 eyePos;
float calcShadow(vec3 normal, vec3 lightDir){
    vec3 lightSpaceProj = fragPosLightSpace.xyz / fragPosLightSpace.w;
    lightSpaceProj = lightSpaceProj * 0.5 + 0.5;
    float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.05);
    float currentDepth = lightSpaceProj.z;
    if(currentDepth > 1.0)
    return 1.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for(int x = -1; x <= 1; ++x){
        for(int y = -1; y <= 1; ++y){
            float depth = texture(shadowMap, lightSpaceProj.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > depth ? 0.8 : 1.0;
        }
    }
    return shadow / 9.0;
}
void main(){
    vec4 diffuseColor;
    if(useColor) diffuseColor = color;
    else diffuseColor = texture(tex, tCoords);
    vec3 ambientLighting = sun.color * sun.ambientFac;
    vec3 lightDir = normalize(sun.position - fragPos);
    vec3 normal = normalize(norm);
    float diffFactor = max(dot(normal, lightDir), 0.1);
    vec3 diffuseLighting = diffFactor * sun.color * sun.diffFac;
    if(diffuseColor.rgb == vec3(1, 0, 0)) diffuseLighting += diffuseColor.rgb;

    vec3 reflectDirection = normalize(lightDir + normalize(eyePos - fragPos));
    float spec = pow(max(dot(normal, reflectDirection), 0.0), 8.0);
    vec3 specularLighting = spec * sun.specFac * sun.color;
    glFragColor = vec4((ambientLighting + diffuseLighting + specularLighting) * (calcShadow(normal, lightDir)) * diffuseColor.rgb, diffuseColor.a);
}
