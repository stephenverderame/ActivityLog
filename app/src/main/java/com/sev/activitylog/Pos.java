package com.sev.activitylog;

import java.util.ArrayList;

public class Pos {
    public double lat, lon;
    public Pos() {};
    public Pos(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    public static ArrayList<Pos> decodePolyline(String polyline, int precision) {
        ArrayList<Pos> points = new ArrayList<Pos>();
        Intptr i = new Intptr();
        i.value = 0;
        int lat = 0, lon = 0;
        while(i.value < polyline.length()){
            int latdiff = decodeHelper(polyline, i);
            int londiff = decodeHelper(polyline, i);
            lat += latdiff;
            lon += londiff;
            points.add(new Pos((double)lat / Math.pow(10, precision), (double)lon / Math.pow(10, precision)));
        }
        return points;

    }
    private static int decodeHelper(String polyline, Intptr index){
        int shift = 0, result = 0;
        byte bit = 0;
        do{
            bit = (byte)(polyline.charAt(index.value++) - 63);
            result |= (bit & 0x1f) << shift;
            shift += 5;
        } while(bit >= 0x20);
        return ((result & 1) == 1) ? ~(result >> 1) : (result >> 1);
    }
}
//Porting C++ code, probably a better Java way to do this
class Intptr{
    public int value;
}
