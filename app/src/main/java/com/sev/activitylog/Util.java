package com.sev.activitylog;

public class Util {
    public static int rgba(float r, float g, float b, float a){
        return ((int)(a * 0xFF) << 24) | ((int)(r * 0xFF) << 16) | ((int)(g * 0xFF) << 8) | ((int)(b * 0xFF));
    }
}
