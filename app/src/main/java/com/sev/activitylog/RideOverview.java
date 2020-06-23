package com.sev.activitylog;


import java.util.Date;

public class RideOverview {
    private double distance, climbed;
    private int time, totalTime;
    private String name;
    private Date date;
    private String id;
    public RideOverview(String name, String id){
        this.name = name;
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
    public String getName() {return name;}
    public Date getDate() {return date;}
    public double getDistance() {return distance;}
    public int getMovingTime() {return time;}
    public double getClimbed() {return climbed;}


}
