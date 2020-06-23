package com.sev.activitylog;

public class ActivityViewFontBuilder {
    private ActivityViewFont font; //units are dps
    ActivityViewFontBuilder(){
        font = new ActivityViewFont();
    }
    ActivityViewFontBuilder titleSize(float s){
        font.titleSize = s;
        return this;
    }
    ActivityViewFontBuilder subtitleSize(float s){
        font.subtitleSize = s;
        return this;
    }
    ActivityViewFontBuilder labelSize(float s){
        font.labelSize = s;
        return this;
    }
    ActivityViewFontBuilder infoSize(float s){
        font.infoSize = s;
        return this;
    }
    ActivityViewFont build(){
        return font;
    }
}
