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
    public DetailedRide(JSONObject ride) throws JSONException, ParseException {
        init(ride);
    }
    private void init(JSONObject ride) throws JSONException, ParseException {
        basicInfo = new RideOverview(ride);
        JSONObject map = ride.getJSONObject("map");
        polyline = map.getString("polyline");
        description = ride.getString("description");
        Pos p = new Pos();
        try {
            JSONObject gear = ride.getJSONObject("gear");
            gearName = gear.getString("name");
            JSONArray jsonPos = ride.getJSONArray("start_latlng");
            p.lat = jsonPos.getDouble(0);
            p.lon = jsonPos.getDouble(1);
            start = p;
            jsonPos = ride.getJSONArray("end_latlng");
            p.lat = jsonPos.getDouble(0);
            p.lon = jsonPos.getDouble(1);
            end = p;
        }catch (JSONException e){
            //Not a ride
        }
    }
    public RideOverview getOverview() {return basicInfo;}
    public String getGearName() {return gearName;}
    public String getDesc() {return description;}
    public ArrayList<Pos> getRoute() {
        return polyline == null || polyline.equalsIgnoreCase("null") || polyline.length() == 0 ? null : Pos.decodePolyline(polyline, 5);}

    public void setOverview(RideOverview ov) {basicInfo = ov;}
}
