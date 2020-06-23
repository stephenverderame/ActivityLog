package com.sev.activitylog;

public class PaddingBuilder {
    private Padding padding; //units are dps
    public PaddingBuilder(){
        padding = new Padding();
    }
    public PaddingBuilder top(int t){
        padding.top = t;
        return this;
    }
    public PaddingBuilder bottom(int t){
        padding.bottom = t;
        return this;
    }
    public PaddingBuilder left(int t){
        padding.left = t;
        return this;
    }
    public PaddingBuilder right(int t){
        padding.right = t;
        return this;
    }
    public Padding build() {return padding;}
}
