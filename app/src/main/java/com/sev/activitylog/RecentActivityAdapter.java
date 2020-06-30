package com.sev.activitylog;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.CardListViewHolder> implements Subject {
    private LinkedList<RideOverview> data;
    private LinkedList<Observer> observers;
    public RecentActivityAdapter(LinkedList<RideOverview> data){
        this.data = data;
        observers = new LinkedList<>();
    }
    public RecentActivityAdapter(ArrayList<RideOverview> data){
        this.data = new LinkedList<RideOverview>();
        this.data.addAll(data);
        observers = new LinkedList<>();
    }
    @NonNull
    @Override
    public CardListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_item, parent, false);
        return new CardListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CardListViewHolder holder, int position) {
        TextView title = (TextView)holder.cardItemView.findViewById(R.id.cardTitle);
        title.setText(data.get(position).getName());
        ((TextView)holder.cardItemView.findViewById(R.id.cardDate)).setText(new SimpleDateFormat("MMM dd yyyy hh:mm a").format(data.get(position).getDate()));
        ((TextView)holder.cardItemView.findViewById(R.id.cardActivityType)).setText(data.get(position).getActivityType());
//        ((TextView)holder.cardItemView.findViewById(R.id.cardGear)).setText(data.get(position).getGearId());
        ((TextView)holder.cardItemView.findViewById(R.id.cardDistance)).setText(String.format("%.2f miles", data.get(position).getDistance() * 0.000621371));
        ((TextView)holder.cardItemView.findViewById(R.id.cardElevation)).setText(String.format("%.2f ft", data.get(position).getClimbed() * 3.28084));
        ((TextView)holder.cardItemView.findViewById(R.id.cardTime)).setText(TimeSpan.fromSeconds((long)data.get(position).getMovingTime()));

        holder.cardItemView.setOnClickListener((View v) -> {
            Log.d("ADAPTER", "Click!");
            Log.d("ADAPTER", String.valueOf(observers.size()));
            for(Observer o : observers){
                o.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_NOTIFY, data.get(position).getId(), v));
            }
        });


    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    public static class CardListViewHolder extends RecyclerView.ViewHolder {

        public View cardItemView;
        public CardListViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItemView = itemView;
        }

    }

}
class WeekViewAdapter extends RecyclerView.Adapter<WeekViewAdapter.WeekViewHolder> implements Subject {
    private ArrayList<BasicRideData> list;
    private long totalDays;
    private int numWeeks;
    private int latestDayOfWeek, firstDayOfWeek;
    private LinkedList<RideOverview> rides;
    private LinkedList<Observer> observers;
    public WeekViewAdapter(LinkedList<RideOverview> rides){
        list = new ArrayList<BasicRideData>();
        observers = new LinkedList<>();
        this.rides = rides;
    }
    public void init(){
        int j = 0;
        list.add(new BasicRideData(rides.get(0)));
        for(int i = 1; i < rides.size(); ++i){
            RideOverview prev = rides.get(i);
            if(isSameDay(rides.get(i - 1).getDate().getTime(), prev.getDate().getTime())){
                list.set(j, list.get(j).add(rides.get(i)));
            }else{
                list.add(new BasicRideData(rides.get(i)));
                ++j;
            }

        }
        long timeBetween = System.currentTimeMillis() - rides.get(rides.size() - 1).getDate().getTime();
        totalDays = TimeUnit.DAYS.convert(timeBetween, TimeUnit.MILLISECONDS);
        numWeeks = (int)Math.ceil(totalDays / 7.f);
        Calendar now = Calendar.getInstance();
        now.setTime(new Date()); //sets dat to now
        latestDayOfWeek = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7; //convert Sunday (1) to Saturday (7) to Monday (0) to Sunday (7)
        now.setTime(rides.get(rides.size() - 1).getDate());
        firstDayOfWeek = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }
    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.week_view, parent, false);
        return new WeekViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        ArrayList<BasicRideData> rides = getActivityWeek(position);
        long day = (System.currentTimeMillis() - (latestDayOfWeek * (1000 * 3600 * 24))) - (long)position * 1000 * 3600 * 24 * 7; //epoch time of start date
        Date startDate = new Date(day);
        ((TextView)holder.v.findViewById(R.id.weekDate)).setText("Week of " + new SimpleDateFormat("MMM dd yyyy").format(startDate));
        double distance = 0, elevation = 0;
        int time = 0;
        for(int i = 0, j = rides.size() - 1; i < 7 && j >= 0; ++i){
            long dayToAdd = day + i * 1000 * 3600 * 24;
            View view = holder.days[i];
            int activityTime = 0;
            if(isSameDay(rides.get(j).date.getTime(), dayToAdd)){
                ((TextView)view.findViewById(R.id.dayTime)).setText(TimeSpan.fromSeconds(rides.get(j).time));
                ((TextView)view.findViewById(R.id.dayDistance)).setText(String.format("%.2f miles", rides.get(j).distance));
                distance += rides.get(j).distance;
                time += rides.get(j).time;
                elevation += rides.get(j).elevation;
                activityTime = rides.get(j).time;
                final int clickListenerId = j;
                --j;

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(rides.get(clickListenerId).rides.size() > 1) {
                            for (Observer o : observers) {
                                o.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_MULTIPLE_NOTIFY, rides.get(clickListenerId).rides, view));
                            }
                        }else{
                            for (Observer o : observers) {
                                o.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_NOTIFY, rides.get(clickListenerId).rides.get(0).getId(), view));
                            }
                        }

                    }
                });
            }else if(rides.get(j).date.after(new Date(dayToAdd))){
                ((TextView)view.findViewById(R.id.dayDistance)).setText("Rest");
                ((TextView)view.findViewById(R.id.dayTime)).setText("");
                activityTime = 0;
            }
        }
        ((TextView)holder.v.findViewById(R.id.weekTime)).setText(TimeSpan.fromSeconds(time));
        ((TextView)holder.v.findViewById(R.id.weekDistance)).setText(String.format("%.2f miles", distance));
        ((TextView)holder.v.findViewById(R.id.weekElevation)).setText(String.format("%.2f ft", elevation));
    }

    @Override
    public int getItemCount() {
        return numWeeks;
    }

    /**
     * Gets all the activities that occurred in the week relative to today.
     * Binary searches through rideList based on date
     * @param weekFromToday 0 indexed number identifying the week from today to fetch
     * @return list of activities ordered most recent to earlier (Sun to Mon)
     */
    private ArrayList<BasicRideData> getActivityWeek(int weekFromToday){
        ArrayList<BasicRideData> weekRides = new ArrayList<>();
        Date endDate = new Date((System.currentTimeMillis() + (6 - latestDayOfWeek) * (long)(1000 * 3600 * 24)) - (long)weekFromToday * 1000 * 3600 * 24 * 7); //first term casts to long bc of System.currentTimeMillis(), second term of subtraction does not implicitly
        Date startDate = new Date((System.currentTimeMillis() - (latestDayOfWeek * (long)(1000 * 3600 * 24))) - (long)weekFromToday * 1000 * 3600 * 24 * 7);
        long key = (endDate.getTime() + startDate.getTime()) / 2;
        int id = 0;
        int start = 0, end = list.size() - 1; //start and end in memory not time
        //items towards end of list is later
        do {
            id = (int) Math.round((list.get(start).date.getTime() - key) / (double) (list.get(start).date.getTime() - list.get(end).date.getTime()) * (end - start) + start);
            if(id < start) id = start;
            else if(id >= end) id = end;
            if((list.get(id).date.after(startDate) || isSameDay(list.get(id).date, startDate)) &&
                    (list.get(id).date.before(endDate) || isSameDay(list.get(id).date, endDate))){
                for(int i = Math.max(0, id - 7); i < Math.min(list.size(), id + 8); ++i){
                    if((list.get(i).date.after(startDate) || isSameDay(startDate, list.get(i).date)) &&
                            (list.get(i).date.before(endDate) || isSameDay(list.get(i).date, endDate))){
                        weekRides.add(list.get(i));
                    }
                }
                break;
            }
            else if(list.get(id).date.before(startDate)){
                end = id - 1;
            }else if(list.get(id).date.after(endDate)){
                start = id + 1;
            }
        } while(start <= end && start < list.size() && end < list.size());
        return weekRides;
    }
    private boolean isSameDay(long time1, long time2){
        return isSameDay(new Date(time1), new Date(time2));
    }
    private boolean isSameDay(Date time1, Date time2){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(time1).equals(fmt.format(time2));
    }
    private boolean isSameDay(long time1, Date time2){
        return isSameDay(new Date(time1), time2);
    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observers);
    }

    public static class WeekViewHolder extends RecyclerView.ViewHolder {

        View v;
        View days[];
        private static final String dayOfWeek[] = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        public WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            days = new View[7];
            LinearLayout row = v.findViewById(R.id.weekContainer);
            for(int i = 0; i < 7; ++i) {
                days[i] = LayoutInflater.from(v.getContext()).inflate(R.layout.week_item, row, false);
                ((TextView)days[i].findViewById(R.id.dayName)).setText(dayOfWeek[i]);
                row.addView(days[i]);
            }
        }
    }
    private class BasicRideData {
        public Date date;
        public String activity;
        public double distance, elevation;
        public int time;
        public ArrayList<RideOverview> rides;
        public BasicRideData(RideOverview r){
            rides = new ArrayList<RideOverview>();
            date = r.getDate();
            distance = r.getDistance() * RideOverview.METERS_MILES_CONVERSION;
            time = r.getMovingTime();
            activity = r.getActivityType();
            elevation = r.getClimbed() * RideOverview.METERS_FEET_CONVERSION;
            rides.add(r);
        }
        public BasicRideData(Date d){
            date = d;
        }
        public BasicRideData add(RideOverview r){
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
            if(fmt.format(date).equals(r.getDate())){
                distance += r.getDistance();
                time += r.getMovingTime();
                rides.add(r);
            }
            return this;
        }

    }
}
