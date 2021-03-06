package com.sev.activitylog;

//basic linear algebra helper functions
public class GLM {
    public static final int FLOAT_SIZE = 4;
    public static final int INT_SIZE = 4;
    static float[] cross(final float[] a, final float[] b){
        assert(a.length == 3 && b.length == 3);
        float[] res = new float[3];
        res[0] = a[1] * b[2] - a[2] * b[1];
        res[1] = a[2] * b[0] - a[0] * b[2];
        res[2] = a[0] * b[1] - a[1] * b[0];
        return res;
    }
    static float length(final float[] a){
        return (float)Math.sqrt(dot(a, a));
    }
    static float[] normalize(final float[] a){
        float[] out = new float[a.length];
        float length = length(a);
        for(int i = 0; i < a.length; ++i)
            out[i] = a[i] / length;
        return out;
    }
    static float dot(final float[] a, final float[] b){
        assert(a.length == b.length);
        float d = 0;
        for(int i = 0; i < a.length; ++i){
            d += a[i] * b[i];
        }
        return d;
    }
    static float angleBetween(final float[] a, final float[] b){
        return (float)Math.acos(dot(a, b) / (length(a) * length(b)));
    }
    static float[] add(final float[] a, final float[] b){
        assert(a.length == b.length);
        float[] res = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            res[i] = a[i] + b[i];
        return res;
    }
    //resulting vector will point towards a
    static float[] subtract(final float[] a, final float[] b){
        assert(a.length == b.length);
        float[] res = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            res[i] = a[i] - b[i];
        return res;
    }
    static float[] scale(final float[] a, float scaler){
        float[] res = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            res[i] = a[i] * scaler;
        return res;
    }
    static float[] negate(final float[] a){
        float[] out = new float[a.length];
        for(int i = 0; i < a.length; ++i){
            out[i] = -a[i];
        }
        return out;
    }
    static boolean equals(final float[] a, final float[] b) {
        assert (a.length == b.length);
        for (int i = 0; i < a.length; ++i){
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
    static float dist(final float[] a, final float[] b){
        final float[] c = subtract(a, b);
        return length(c);
    }
    static float[] copy(final float[] a){
        float[] b = new float[a.length];
        for(int i = 0; i < a.length; ++i)
            b[i] = a[i];
        return b;
    }
    static float[] resize(final float[] a, int size, float defaultValue){
        if(a.length == size) return a;
        else{
            float[] b = new float[size];
            for(int i = 0; i < size; ++i){
                if(i < a.length) b[i] = a[i];
                else b[i] = defaultValue;
            }
            return b;
        }
    }
}
