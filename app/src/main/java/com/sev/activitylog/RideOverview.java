package com.sev.activitylog;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RideOverview implements Serializable {
    public static transient final double METERS_MILES_CONVERSION = 0.000621371;
    public static transient final double METERS_FEET_CONVERSION = 3.28084;
    private double distance, climbed;
    private int time, totalTime;
    private String name;
    private Date date;
    private long id;
    private String gearId;
    private String activityType;
    private float maxSpeed, averageSpeed;
    private float maxWatts, avgWatts, work;
    public RideOverview(String name, long id){
        this.name = name;
        this.id = id;
    }
    public RideOverview(JSONObject ride) throws JSONException, ParseException {
        name = ride.getString("name");
        id = ride.getLong("id");
        setDistance(ride.getDouble("distance"));
        setMovingTime(ride.getInt("moving_time"));
        setTotalTime(ride.getInt("elapsed_time"));
        Date rideDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(ride.getString("start_date_local"));
        setDate(rideDate);
        setClimbed(ride.getDouble("total_elevation_gain"));
        setAvgSpeed((float) ride.getDouble("average_speed"));
        setMaxSpeed((float) ride.getDouble("max_speed"));
        if (ride.has("average_watts"))
            setPower((float) ride.getDouble("average_watts"));
        if (ride.has("max_watts"))
            setMaxPow((float) ride.getDouble("max_watts"));
        if (ride.has("kilojoules"))
            setKJ((float) ride.getDouble("kilojoules"));
        setActivityType(ride.getString("type"));
    }
    public void setMovingTime(int time) {
        this.time = time;
    }
    public void setDistance(double dist){
        this.distance = dist;
    }
    public void setTotalTime(int time){
        this.totalTime = time;
    }
    public void setDate(Date date){
        this.date = date;
    }
    public void setClimbed(double e) {climbed = e;}
    public void setGearId(String gid) {gearId = gid;}
    public void setActivityType(String type) {activityType = type;}
    public String getName() {return name;}
    public Date getDate() {return date;}
    public double getDistance() {return distance;}
    public int getMovingTime() {return time;}
    public double getClimbed() {return climbed;}
    public String getGearId() {return gearId;}
    public String getActivityType() {return activityType;}
    public long getId() {return id;}
    public void setAvgSpeed(float avg) {averageSpeed = avg;}
    public void setMaxSpeed(float mx) {maxSpeed = mx;}
    public void setPower(float avgPower) {avgWatts = avgPower;}
    public void setMaxPow(float mx) {maxWatts = mx;}
    public void setKJ(float kilojoules) {work = kilojoules;}
    public float getAverageSpeed() {return averageSpeed;}
    public float getPower() {return avgWatts;}
    public int getTotalTime() {return totalTime;}


}
