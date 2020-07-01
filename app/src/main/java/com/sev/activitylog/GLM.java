package com.sev.activitylog;

public class GLM {
    public static final int FLOAT_SIZE = 4;
    public static final int INT_SIZE = 4;
    static float[] cross(float[] a, float[] b){
        assert(a.length == 3 && b.length == 3);
        float[] res = new float[3];
        res[0] = a[1] * b[2] - a[2] * b[1];
        res[1] = a[2] * b[0] - a[0] * b[2];
        res[2] = a[0] * b[1] - a[1] * b[0];
        return res;
    }
    static float length(float[] a){
        return (float)Math.sqrt(dot(a, a));
    }
    static float[] normalize(float[] a){
        float[] out = new float[a.length];
        float length = length(a);
        for(int i = 0; i < a.length; ++i)
            out[i] = a[i] / length;
        return out;
    }
    static float dot(float[] a, float[] b){
        assert(a.length == b.length);
        float d = 0;
        for(int i = 0; i < a.length; ++i){
            d += a[i] * b[i];
        }
        return d;
    }
    static float angleBetween(float[] a, float[] b){
        return (float)Math.acos(dot(a, b) / (length(a) * length(b)));
    }
    static float[] add(float[] a, float[] b){
        assert(a.length == b.length);
        float[] res = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            res[i] = a[i] + b[i];
        return res;
    }
    //resulting vector will point towards a
    static float[] subtract(float[] a, float[] b){
        assert(a.length == b.length);
        float[] res = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            res[i] = a[i] - b[i];
        return res;
    }
    static float[] scale(float[] a, float scaler){
        float[] res = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            res[i] = a[i] * scaler;
        return res;
    }
    static float[] mat4toMat3(float[] mat4){
        assert(mat4.length == 16);
        float[] mat3 = new float[9];
        for(int i = 0; i < 3; ++i){
            for(int j = 0; j < 3; ++j){
                mat3[i * j] = mat4[i * 4 + j];
            }
        }
        return mat3;
    }
    static float[] negate(float[] a){
        float[] out = new float[a.length];
        for(int i = 0; i < a.length; ++i){
            out[i] = -a[i];
        }
        return out;
    }
}
