package com.sev.activitylog;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StatsFragment extends DataFragment implements View.OnClickListener {
    private View view;
    private RideOverview master;
    private ArrayList<Integer> filterIndices;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.stats_layout, null);
            view.findViewById(R.id.custom_btn).setOnClickListener(this);
            init(filterIndices);
        }
        return view;
    }
    public StatsFragment(List<RideOverview> data){
        super(data, null);
    }
    public void init(ArrayList<Integer> data){
        int count = data == null ? this.data.size() : data.size();
        master = new RideOverview();
        if(data != null) {
            for (int d : data) {
                RideOverview r = this.data.get(d);
                addRideData(master, r);
            }
        }else{
            for(RideOverview r : this.data){
                addRideData(master, r);
            }
        }
        master.setPower(master.getPower() / count);
        master.setAvgSpeed(master.getAverageSpeed() / count);
        setText(R.id.stats_distance, String.format("Distance: %.2f miles", master.getDistance() * RideOverview.METERS_MILES_CONVERSION));
        setText(R.id.stats_activities, "Activities: " + Integer.toString(count));
        setText(R.id.stats_avg_distance, String.format("Avg Distance: %.2f miles", master.getDistance() * RideOverview.METERS_MILES_CONVERSION / count));
        setText(R.id.stats_mtime, "Moving Time: " + TimeSpan.fromSeconds(master.getMovingTime()));
        setText(R.id.stats_ttime, "Total Time: " + TimeSpan.fromSeconds(master.getTotalTime()));
        setText(R.id.stats_elevation, String.format("Elevation: %.2f ft", master.getClimbed() * RideOverview.METERS_FEET_CONVERSION));
        setText(R.id.stats_speed, String.format("Avg Speed: %.2f mph", master.getAverageSpeed() * RideOverview.METERS_MILES_CONVERSION * 3600));
        setText(R.id.stats_power, String.format("Avg Power: %.2f W", master.getPower()));
        setText(R.id.stats_calories, String.format("Calories: %.2f kCal", master.getPower() * master.getMovingTime() / 4.184 / 1000 * 4)); //25% efficient at converting cal to work (hence the * 4)
        setText(R.id.stats_avg_time, TimeSpan.fromSeconds(master.getMovingTime() / count));

        int earliest = data == null ? this.data.size() - 1 : data.get(data.size() - 1);
        long timeBetween = System.currentTimeMillis() - this.data.get(earliest).getDate().getTime();
        int totalDays = (int)TimeUnit.DAYS.convert(timeBetween, TimeUnit.MILLISECONDS);
        int numWeeks = (int) Math.ceil(totalDays / 7.f);
        Calendar now = Calendar.getInstance();
        now.setTime(new Date()); //sets date to now
        setText(R.id.stats_week_activities, String.format("Weekly Activities: %.1f", (float)count / numWeeks));

    }
    private void setText(int v, String text){
        ((TextView)view.findViewById(v)).setText(text);
    }

    @Override
    public void filter(SearchFilters filter) {
        filterIndices = new ArrayList<>();
        int start = filter.isDefaultValue(filter.start) ? data.size() - 1 : RideOverview.indexOf(data, filter.start);
        int end = filter.isDefaultValue(filter.end) ? 0 : RideOverview.indexOf(data, filter.end);
        for (; end <= start; ++end) {
            if (data.get(end).doesApply(filter)) filterIndices.add(end);
        }
        init(filterIndices);
        view.invalidate(); //calls on create view again
    }

    @Override
    public void notifyDataInsertion(int index, int count) {
        notifyDataChanged();
    }

    @Override
    public void notifyDataChanged(int pos, int count) {
        notifyDataChanged();
    }

    @Override
    public void notifyDataFinish() {
        notifyDataChanged();
    }

    @Override
    public void notifyDataChanged() {
        if(view != null){
            init(filterIndices);
            view.invalidate();
        }
    }

    @Override
    public void loadStateFromParcel(Parcelable state) {
        filterIndices = ((StatMemento)state).getIndices();
        if(view != null) {
            init(filterIndices);
            view.invalidate();
        }
    }

    @Override
    public Parcelable getState() {
        return new StatMemento(filterIndices);
    }

    @Override
    public void cacheState() {
        //caching done automatically
    }

    @Override
    //Custom Calculate button click
    public void onClick(View v) {
        String func = ((Spinner)view.findViewById(R.id.function_spinner)).getSelectedItem().toString();
        String interval = ((Spinner)view.findViewById(R.id.interval_spinner)).getSelectedItem().toString();
        int intervalAmount = 1;
        if(((EditText)view.findViewById(R.id.interval_number)).getText().toString().length() >= 1){
            intervalAmount = Integer.parseInt(((EditText)view.findViewById(R.id.interval_number)).getText().toString());
        }
        String stat = ((Spinner)view.findViewById(R.id.stat_spinner)).getSelectedItem().toString();
        ArrayList<RideOverview> intervalRides = new ArrayList<>();
        long start = data.get(0).getDate().getTime();
        long intervalTime = computeIntervalSeconds(intervalAmount, interval);
        int i = 0, intervals = 1;
        RideOverview master = new RideOverview();
        do{
            if(data.get(i).getDate().getTime() > start - intervals * intervalTime * 1000){
                addRideData(master, data.get(i++));
            }else{
                intervalRides.add(master);
                intervals++;
                master = new RideOverview();
            }
        } while(i < data.size());
        double[] data = new double[intervalRides.size()];
        for(i = 0; i < intervalRides.size(); ++i){
            intervalRides.get(i).setAvgSpeed(intervalRides.get(i).getAverageSpeed() / intervalRides.get(i).getId());
            intervalRides.get(i).setPower(intervalRides.get(i).getPower() / intervalRides.get(i).getId());
            data[i] = getStat(intervalRides.get(i), stat);
        }
        double val = computeFunction(data, func);
        ((TextView)view.findViewById(R.id.custom_result)).setText(formatOutput(val, stat));

    }
    private long computeIntervalSeconds(int num, String interval){
        if(interval.equalsIgnoreCase("Activity")) return 0;
        else if(interval.equalsIgnoreCase("Day")) return num * 3600 * 24;
        else if(interval.equalsIgnoreCase("Week")) return num * 7 * 3600 * 24;
        else if(interval.equalsIgnoreCase("Month")) return num * 4 * 7 * 3600 * 24;
        else if(interval.equalsIgnoreCase("Year")) return num * 52 * 7 * 3600 * 24;
        return 0;
    }
    private double getStat(RideOverview r, String stat){
        if(stat.equalsIgnoreCase("Activities")) return r.getId();
        else if(stat.equalsIgnoreCase("Average Speed")) return r.getAverageSpeed();
        else if(stat.equalsIgnoreCase("Calories")) return r.getKJ() / 4.184 * 4;
        else if(stat.equalsIgnoreCase("Distance")) return r.getDistance();
        else if(stat.equalsIgnoreCase("Elevation")) return r.getClimbed();
        else if(stat.equalsIgnoreCase("Moving Time")) return r.getMovingTime();
        else if(stat.equalsIgnoreCase("Total Time")) return r.getTotalTime();
        else if(stat.equalsIgnoreCase("Power")) return r.getPower();
        else if(stat.equalsIgnoreCase("Max Speed")) return r.getMaxSpeed();
        else throw new IllegalArgumentException(stat + " is not a defined field!");
    }
    private double computeFunction(double[] values, String function){
        double output = 0;
        if(function.equalsIgnoreCase("Mean")){
            for(double v : values)
                output += v;
            output /= values.length;
        }else if(function.equalsIgnoreCase("Summation")){
            for(double v : values)
                output += v;
        }else if(function.equalsIgnoreCase("Maximum")){
            output = Integer.MIN_VALUE;
            for(double v : values)
                if(v > output) output = v;
        }else if(function.equalsIgnoreCase("Minimum")){
            output = Integer.MAX_VALUE;
            for(double v : values)
                if(v < output) output = v;
        }
        else if(function.equalsIgnoreCase("Mode")){
            HashMap<Integer, Integer> map = new HashMap<>();
            for(double v : values) {
                if (map.containsKey(v)) map.put((int)Math.round(v), map.get((int)Math.round(v)) + 1);
                else map.put((int)Math.round(v), 1);
            }
            int max = Integer.MIN_VALUE;
            for(double v : values){
                if(map.get((int)Math.round(v)) > max){
                    output = v;
                    max = map.get((int)Math.round(v));
                }
            }
        }else if(function.equalsIgnoreCase("Median")){
            //O(n) median implemented similarly to quicksort. See algorithm notes for explanation
            int pivot = partition(values, 0, values.length - 1);
            int low = 0, high = values.length - 1;
            while(pivot != values.length / 2){
                if(pivot > values.length / 2)
                    pivot = partition(values, low, pivot - 1);
                else
                    pivot = partition(values, pivot + 1, high);
            }
            output = values[pivot];
        }else throw new IllegalArgumentException(function + " is undefined!");
        return output;
    }
    private String formatOutput(double val, String stat){
        String output = null;
        if(stat.contains("Time")){
            output = TimeSpan.fromSeconds((long)val);
        }else if(stat.equalsIgnoreCase("Activity Type")){
            String[] types = getResources().getStringArray(R.array.activity_type_list);
            output = types[(int)val];
        }else if(stat.equalsIgnoreCase("Distance")){
            output = String.format("%.2f miles", val * RideOverview.METERS_MILES_CONVERSION);
        }else if(stat.equalsIgnoreCase("Elevation")){
            output = String.format("%.2f feet", val * RideOverview.METERS_FEET_CONVERSION);
        }
        else if(stat.contains("Speed")){
            output = String.format("%.2f mph", val * RideOverview.METERS_MILES_CONVERSION * 3600);
        }
        else if(stat.equalsIgnoreCase("Power")){
            output = String.format("%.2f W", val);
        }
        else if(stat.equalsIgnoreCase("Calories"))
            output = String.format("%.2f kCal", val);
        else
            output = String.format("%.2f", val);
        return output;
    }

    /**
     * Adds the stats of r to the stats of master
     * @param master the overview to be modified
     * @param r the overview whose stats will be added to the master overview
     */
    private void addRideData(RideOverview master, RideOverview r){
        master.setMovingTime(master.getMovingTime() + r.getMovingTime());
        master.setTotalTime(master.getTotalTime() + r.getTotalTime());
        master.setClimbed(master.getClimbed() + r.getClimbed());
        master.setAvgSpeed(master.getAverageSpeed() + r.getAverageSpeed());
        master.setDistance(master.getDistance() + r.getDistance());
        master.setPower(master.getPower() + r.getPower());
        master.setKJ(master.getKJ() + r.getPower() * r.getMovingTime() / 1000.f);
        master.setId(master.getId() + 1); //using to count additions
    }

    /**
     * Partitions the list into two sections: values below the pivot and values above the pivot
     * Helper function for fast median
     * @param list
     * @param low index of starting location
     * @param high index of ending location
     * @return the index of where the range between low and high is separated (the index of the pivot)
     */
    private int partition(double[] list, int low, int high){
        int pivot = high;
        int firstHigh = low;
        for(int i = low; i < high; ++i){
            if(list[i] < list[pivot]){
                double temp = list[firstHigh];
                list[firstHigh++] = list[i];
                list[i] = temp;
            }
        }
        double temp = list[firstHigh];
        list[firstHigh] = list[pivot];
        list[pivot] = temp;
        return firstHigh;
    }

}
class StatMemento implements Parcelable {

    private ArrayList<Integer> indices;
    public StatMemento(ArrayList<Integer> indices){
        this.indices = indices;
    }
    public StatMemento(Parcel in) {
        indices = (ArrayList<Integer>)in.readSerializable();
    }

    public static final Creator<StatMemento> CREATOR = new Creator<StatMemento>() {
        @Override
        public StatMemento createFromParcel(Parcel in) {
            return new StatMemento(in);
        }

        @Override
        public StatMemento[] newArray(int size) {
            return new StatMemento[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(indices);
    }
    public ArrayList<Integer> getIndices() {
        return indices;
    }
}
