package com.sev.activitylog;

import java.io.Serializable;

/**
 * Workaround for inability to have arrays of generics
 * @see ActivityView
 */
public class StringPair {
    private String key, value;
    public StringPair(String key, String value){
        this.key = key;
        this.value = value;
    }
    public StringPair(){}
    public String getKey() {return key;}
    public void setKey(String key) {this.key = key;}
    public String getValue() {return value;}
    public void setValue(String value) {this.value = value;}

}
//Serializeable version of android.Pair
class Tuple <K, V> implements Serializable {
    public K first;
    public V second;
    Tuple(K first, V second){
        this.first = first;
        this.second = second;
    }

}
