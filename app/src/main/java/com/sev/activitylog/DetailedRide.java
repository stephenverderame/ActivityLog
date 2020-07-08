package com.sev.activitylog;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;

public class DetailedRide implements Serializable {
    private transient RideOverview basicInfo; //already saved in other database
    private String polyline;
    private String description;
    private String gearName;
    private Pos start, end;
    private MapImage img;
    private Pos[][] heightMap;
    public DetailedRide(JSONObject ride) throws JSONException, ParseException {
        init(ride);
    }
    private void init(JSONObject ride) throws JSONException, ParseException {
        basicInfo = new RideOverview(ride);
        JSONObject map = ride.getJSONObject("map");
        polyline = map.getString("polyline");
        JSONObject gear = ride.getJSONObject("gear");
        gearName = gear.getString("name");
        description = ride.getString("description");
        Pos p = new Pos();
        JSONArray jsonPos = ride.getJSONArray("start_latlng");
        p.lat = jsonPos.getDouble(0);
        p.lon = jsonPos.getDouble(1);
        start = p;
        jsonPos = ride.getJSONArray("end_latlng");
        p.lat = jsonPos.getDouble(0);
        p.lon = jsonPos.getDouble(1);
        end = p;
    }
    public RideOverview getOverview() {return basicInfo;}
    public String getGearName() {return gearName;}
    public String getDesc() {return description;}
    public ArrayList<Pos> getRoute() {return Pos.decodePolyline(polyline, 5);}

    public MapImage getImg() {return img;}
    public Pos[][] getHeightMap() {return heightMap;}
    public void setImg(MapImage bmp) {img = bmp;}
    public void setHeightMap(Pos[][] hm) {heightMap = hm;}
    public void setOverview(RideOverview ov) {basicInfo = ov;}
}
