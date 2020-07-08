package com.sev.activitylog;

//Passed to the activity view
public class Padding {
    public int left = 0, right = 0, top = 0, bottom = 0;
    //units - device-independent pixels
}
//allows construction of padding without specifying all parameters
class PaddingBuilder {
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
