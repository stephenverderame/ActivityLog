package com.sev.activitylog;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES30;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GLShader implements Destructor {
    private int program, vertex, fragment, geometry;
    public GLShader(Resources ctx, int vertexResourceId, int fragmentResourceId) {
        String vertexCode = null, fragmentCode = null;
        StringBuilder codeBuilder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.openRawResource(vertexResourceId)))){
            String line;
            while((line = reader.readLine()) != null)
                codeBuilder.append(line).append('\n');
            vertexCode = codeBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        codeBuilder = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.openRawResource(fragmentResourceId)))){
            String line;
            while((line = reader.readLine()) != null)
                codeBuilder.append(line).append('\n');
            fragmentCode = codeBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        vertex = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vertex, vertexCode);
        GLES30.glCompileShader(vertex);
        int[] success = new int[1];
        GLES30.glGetShaderiv(vertex, GLES30.GL_COMPILE_STATUS, success, 0);
        if(success[0] == 0){
            Log.e("Vertex Shader", GLES30.glGetShaderInfoLog(vertex));
        }
        fragment = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fragment, fragmentCode);
        GLES30.glCompileShader(fragment);
        GLES30.glGetShaderiv(fragment, GLES30.GL_COMPILE_STATUS, success, 0);
        if(success[0] == 0){
            Log.e("Fragment Shader", GLES30.glGetShaderInfoLog(fragment));
        }
        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertex);
        GLES30.glAttachShader(program, fragment);
        GLES30.glLinkProgram(program);
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, success, 0);
        if(success[0] == 0)
            Log.e("Shader Link", GLES30.glGetProgramInfoLog(program));
    }
    public void use(){
        GLES30.glUseProgram(program);
    }
    public void setMat4(String name, float[] matrix){
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(program, name), 1, false, matrix, 0);
    }
    public void setMat3(String name, float[] matrix){
        GLES30.glUniformMatrix3fv(GLES30.glGetUniformLocation(program, name), 1, false, matrix, 0);
    }
    public void setInt(String name, int val){
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, name), val);
    }
    public int getAttribute(String name){
        return GLES30.glGetAttribLocation(program, name);
    }
    public void setVec4(String name, float x, float y, float z, float w){
        GLES30.glUniform4f(GLES30.glGetUniformLocation(program, name), x, y, z, w);
    }
    public void setBool(String name, boolean val){
        GLES30.glUniform1i(GLES30.glGetUniformLocation(program, name), val ? 1 : 0);
    }
    public void setFloat(String name, float val){
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, name), val);
    }
    public void setVec3(String name, float[] vec){
        GLES30.glUniform3fv(GLES30.glGetUniformLocation(program, name), 1, vec, 0);
    }
    public void setVec3(String name, float x, float y, float z){
        GLES30.glUniform3f(GLES30.glGetUniformLocation(program, name), x, y, z);
    }

    @Override
    public void destructor() {
        GLES30.glDeleteShader(vertex);
        GLES30.glDeleteShader(fragment);
        GLES30.glDeleteProgram(program);
    }
}