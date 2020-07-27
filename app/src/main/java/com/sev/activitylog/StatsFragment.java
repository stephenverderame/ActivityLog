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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StatsFragment extends DataFragment implements View.OnClickListener {
    private View view;
    private MasterRideOverview master;
    private ArrayList<Integer> filterIndices;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.stats_layout, null);
            view.findViewById(R.id.custom_btn).setOnClickListener(this);
            ((Spinner)view.findViewById(R.id.stat_spinner)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_STAT_LIST));
            ((Spinner)view.findViewById(R.id.function_spinner)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_FUNC_LIST));
            ((Spinner)view.findViewById(R.id.interval_spinner)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_INT_LIST));
            init(filterIndices);
        }
        return view;
    }
    public StatsFragment(List<RideOverview> data){
        super(data, null);
    }
    public void init(ArrayList<Integer> data){
        int count = data == null ? this.data.size() : data.size();
        master = new MasterRideOverview();
        if(data != null) {
            for (int d : data) {
                RideOverview r = this.data.get(d);
                master.addRideData(r);
            }
        }else{
            for(RideOverview r : this.data){
                master.addRideData(r);
            }
        }
        setText(R.id.stats_distance, String.format(Locale.getDefault(), "%s: %.2f %s", getString(R.string.distance), master.getDistance() * Settings.metersDistanceConversion(), Settings.distanceUnits()));
        setText(R.id.stats_activities, getString(R.string.activities) + ": " + Integer.toString(count));
        setText(R.id.stats_avg_distance, String.format(Locale.getDefault(), "%s: %.2f %s", getString(R.string.avg_distance), master.getDistance() * Settings.metersDistanceConversion() / count, Settings.distanceUnits()));
        setText(R.id.stats_mtime, getString(R.string.mv_time) + ": " + TimeSpan.fromSeconds(master.getMovingTime()));
        setText(R.id.stats_ttime, getString(R.string.tt_time) + ": " + TimeSpan.fromSeconds(master.getTotalTime()));
        setText(R.id.stats_elevation, String.format(Locale.getDefault(), "%s: %.2f %s", getString(R.string.elevation), master.getClimbed() * Settings.metersElevationConversion(), Settings.elevationUnits()));
        setText(R.id.stats_speed, String.format(Locale.getDefault(), "%s: %.2f %s", getString(R.string.avg_speed), master.getAverageSpeed() * Settings.metersDistanceConversion() * 3600, Settings.speedUnits()));
        setText(R.id.stats_power, String.format(Locale.getDefault(), "%s: %.2f W", getString(R.string.power), master.getPower()));
        setText(R.id.stats_calories, String.format(Locale.getDefault(), "%s: %.2f kCal", getString(R.string.calories), master.getPower() * master.getMovingTime() / 4.184 / 1000 * 4)); //25% efficient at converting cal to work (hence the * 4)
        setText(R.id.stats_avg_time, TimeSpan.fromSeconds(master.getMovingTime() / count));

        int earliest = data == null ? this.data.size() - 1 : data.get(data.size() - 1);
        long timeBetween = System.currentTimeMillis() - this.data.get(earliest).getDate().getTime();
        int totalDays = (int)TimeUnit.DAYS.convert(timeBetween, TimeUnit.MILLISECONDS);
        int numWeeks = (int) Math.ceil(totalDays / 7.f);
        Calendar now = Calendar.getInstance();
        now.setTime(new Date()); //sets date to now
        setText(R.id.stats_week_activities, String.format(Locale.getDefault(), "%s: %.1f", getString(R.string.week_act), (float)count / numWeeks));

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
        FunctionalSpinnerItem func = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.function_spinner)).getSelectedItem();
        FunctionalSpinnerItem interval = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.interval_spinner)).getSelectedItem();
        int intervalAmount = 1;
        if(((EditText)view.findViewById(R.id.interval_number)).getText().toString().length() >= 1){
            intervalAmount = Integer.parseInt(((EditText)view.findViewById(R.id.interval_number)).getText().toString());
        }
        FunctionalSpinnerItem stat = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.stat_spinner)).getSelectedItem();
        ArrayList<MasterRideOverview> intervalRides = new ArrayList<>();
        long start = data.get(0).getDate().getTime();
        long intervalTime = Util.intervalSeconds(intervalAmount, interval) * 1000L;
        int intervals = 1;
        MasterRideOverview master = new MasterRideOverview();
        master.addRideData(data.get(0));
        for(int i = 1; i < data.size(); ++i){
            if(data.get(i).getDate().getTime() > start - intervals * intervalTime){
                master.addRideData(data.get(i));
            }else{
                intervalRides.add(master);
                master = new MasterRideOverview();
                master.addRideData(data.get(i));
                ++intervals;
            }
        }
        intervalRides.add(master);
        double[] data = new double[intervalRides.size()];
        for(int i = 0; i < intervalRides.size(); ++i){
            data[i] = Util.getStat(intervalRides.get(i), stat);
        }
        double val = Util.computeFunction(data, func);
        ((TextView)view.findViewById(R.id.custom_result)).setText(Util.formatOutput(val, stat));

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
