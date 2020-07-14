package com.sev.activitylog;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RideOverview implements Serializable {
    public static transient final double METERS_MILES_CONVERSION = 0.000621371; //this data isn't serialized
    public static transient final double METERS_FEET_CONVERSION = 3.28084;

    private static final long serialVerionUID = 56746437589L;
    private double distance, climbed;
    private int time, totalTime;
    private String name;
    private Date date;
    private long id;
    private String gearId;
    private String activityType;
    private float maxSpeed, averageSpeed;
    private float maxWatts, avgWatts, work;
    private int exertion;
    private boolean isRace;
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
        gearId = ride.getString("gear_id");
        exertion = 0; isRace = false;
    }
    public RideOverview() {
        distance = climbed = maxSpeed = averageSpeed = maxWatts = avgWatts = work = id = time = totalTime = 0;
        name = "";
        date = new Date(0);
        exertion = 0;
        isRace = false;
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
    public float getMaxSpeed() {return maxSpeed;}
    public float getPower() {return avgWatts;}
    public int getTotalTime() {return totalTime;}
    public void setName(String name) {this.name = name;}
    public float getKJ() {return work;}
    public void setId(long id) {this.id = id;}
    public void setRace(boolean race){
        this.isRace = race;
    }
    public boolean getRace() {return isRace;}
    public void setExertion(int exertion){
        this.exertion = exertion;
    }
    public int getExertion(){
        return exertion;
    }

    /**
     *
     * @param filter
     * @return true if the ride matches the filter
     */
    public boolean doesApply(SearchFilters filter){
        if(!filter.isDefaultValue(filter.name) && !name.toLowerCase().contains(filter.name.toLowerCase())) return false;
        if(!filter.isDefaultValue(filter.start) && filter.start.after(date)) return false;
        if(!filter.isDefaultValue(filter.end) && filter.end.before(date)) return false;
        if(!filter.isDefaultValue(filter.gear) && !filter.gear.equalsIgnoreCase(gearId)) return false;
        if(!filter.isDefaultValue(filter.workoutType) && !filter.workoutType.equalsIgnoreCase(activityType)) return false;

        if(!filter.isDefaultValue(filter.maxDist) && filter.maxDist < distance * METERS_MILES_CONVERSION) return false;
        if(!filter.isDefaultValue(filter.minDist) && filter.minDist > distance * METERS_MILES_CONVERSION) return false;
        if(!filter.isDefaultValue(filter.maxElevation) && filter.maxElevation < climbed * METERS_FEET_CONVERSION) return false;
        if(!filter.isDefaultValue(filter.minElevation) && filter.minElevation > climbed * METERS_FEET_CONVERSION) return false;
        if(!filter.isDefaultValue(filter.maxSpeed) && filter.maxSpeed < averageSpeed * METERS_MILES_CONVERSION * 3600) return false;
        if(!filter.isDefaultValue(filter.minSpeed) && filter.minSpeed > averageSpeed * METERS_MILES_CONVERSION * 3600) return false;
        if(!filter.isDefaultValue(filter.maxTime) && filter.maxTime < time) return false;
        if(!filter.isDefaultValue(filter.minTime) && filter.minTime > time) return false;
        if(!filter.isDefaultValue(filter.maxPow) && filter.maxPow < avgWatts) return false;
        if(!filter.isDefaultValue(filter.minPow) && filter.minPow > avgWatts) return false;
        return true;
    }
    public static boolean isSameDay(long time1, long time2){
        return isSameDay(new Date(time1), new Date(time2));
    }
    public static boolean isSameDay(Date time1, Date time2){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(time1).equals(fmt.format(time2));
    }
    public static boolean isSameDay(Date time1, long time2){
        return isSameDay(time1, new Date(time2));
    }
    public static boolean isSameDay(RideOverview a, RideOverview b){
        return isSameDay(a.date, b.date);
    }
    public static boolean isSameDay(RideOverview a, Date b){
        return isSameDay(a.date, b);
    }

    /**
     * Interpolated binary search to find the index of an activity on the specified date, or location of where that date should be inserted
     * @param rides list of rides, ordered most to least recent
     * @param d date of new ride
     * @return
     */
    public static int indexOf(List<RideOverview> rides, Date d){
        int index, start = 0, end = rides.size() - 1;
        if(rides.get(start).date.before(rides.get(end).date)) throw new IllegalStateException("list should be ordered most recent to least recent");
        do{
            index = (int)Math.round((double)(end - start) / (rides.get(start).date.getTime() - rides.get(end).date.getTime()) * (rides.get(start).date.getTime() - d.getTime()) + start);
            if(index < start) index = start;
            if(index > end) index = end;
            if(isSameDay(rides.get(index), d)) return index;
            else if(rides.get(index).date.before(d)){
                end = index - 1;
            }
            else if(rides.get(index).date.after(d)){
                start = index + 1;
            }
        } while(start <= end);
        return index;
    }

    /**
     * Same as indexOf, except requires an exact match and will return -1 if no activity exists
     * @param rides
     * @param d
     * @return
     */
    public static int indexOfExact(List<RideOverview> rides, Date d){
        int index, start = 0, end = rides.size() - 1;
        if(rides.get(start).date.before(rides.get(end).date)) throw new IllegalStateException("list should be ordered most recent to least recent");
        do{
            index = (int)Math.round((double)(end - start) / (rides.get(start).date.getTime() - rides.get(end).date.getTime()) * (rides.get(start).date.getTime() - d.getTime()) + start);
            if(index < start) index = start;
            if(index > end) index = end;
            if(rides.get(index).date.equals(d)) return index;
            else if(rides.get(index).date.before(d)){
                end = index - 1;
            }
            else if(rides.get(index).date.after(d)){
                start = index + 1;
            }
        } while(start <= end);
        return -1;
    }


}

class Gear implements Serializable {
    public String gearId, gearName;
    public double totalDistance;
    public String brand, model, description;
    public Gear(){};
    public Gear(String gearId){
        this.gearId = gearId;
    }
    public Gear(JSONObject obj) throws JSONException {
        gearName = obj.getString("name");
        totalDistance = obj.getDouble("distance");
        brand = obj.getString("brand_name");
        model = obj.getString("model_name");
        description = obj.getString("description");
        gearId = obj.getString("id");
    }
}
