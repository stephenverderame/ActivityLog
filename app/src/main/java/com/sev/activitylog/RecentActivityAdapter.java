package com.sev.activitylog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.view.View.GONE;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.CardListViewHolder> implements Subject {
    private List<RideOverview> data;
    private LinkedList<Observer> observers;
    public RecentActivityAdapter(List<RideOverview> data){
        this.data = data;
        observers = new LinkedList<>();
    }
    public RecentActivityAdapter(List<RideOverview> data, Observer... observers){
        this(data);
        for(Observer o : observers)
            this.observers.add(o);
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
        ((TextView)holder.cardItemView.findViewById(R.id.cardDate)).setText(new SimpleDateFormat("MMM dd yyyy hh:mm a", Locale.getDefault()).format(data.get(position).getDate()));
        ((TextView)holder.cardItemView.findViewById(R.id.cardActivityType)).setText(data.get(position).getActivityType());
//        ((TextView)holder.cardItemView.findViewById(R.id.cardGear)).setText(data.get(position).getGearId());
        ((TextView)holder.cardItemView.findViewById(R.id.cardDistance)).setText(String.format(Locale.getDefault(), "%.2f %s", data.get(position).getDistance() * Settings.metersDistanceConversion(), Settings.distanceUnits()));
        ((TextView)holder.cardItemView.findViewById(R.id.cardElevation)).setText(String.format(Locale.getDefault(), "%.2f %s", data.get(position).getClimbed() * Settings.metersElevationConversion(), Settings.elevationUnits()));
        ((TextView)holder.cardItemView.findViewById(R.id.cardTime)).setText(TimeSpan.fromSeconds((long)data.get(position).getMovingTime()));

        holder.cardItemView.setOnClickListener((View v) -> {
            Log.d("ADAPTER", "Click!");
            Log.d("ADAPTER", String.valueOf(observers.size()));
            for(Observer o : observers){
                o.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_NOTIFY, data.get(position), v));
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
    private ArrayList<RideOverview> weekOverview;
    private ArrayList<int[]> weekIndexes;
    private long totalDays;
    private int numWeeks;
    private int latestDayOfWeek, firstDayOfWeek;
    private List<RideOverview> rides;
    private LinkedList<Observer> observers;
    private boolean inited;
    private SearchFilters filter;
    private Context ctx;
    private WeekScrollListener scrollListener;
    public WeekViewAdapter(List<RideOverview> rides, Context ctx){
        weekOverview = new ArrayList<>();
        weekIndexes = new ArrayList<>();
        observers = new LinkedList<>();
        this.rides = rides;
        inited = false;
        this.ctx = ctx;
    }
    public WeekViewAdapter(List<RideOverview> rides, SearchFilters filter, Context ctx){
        this(rides, ctx);
        this.filter = filter;
    }
    public WeekViewAdapter(List<RideOverview> rides, SearchFilters filter, Context ctx, Observer... observers){
        this(rides, filter, ctx);
        for(Observer o : observers)
            this.observers.add(o);
        init();
    }

    /**
     * Initializes week adapter
     * requires that all the ride data is loaded
     * Partitions rides into weeks and sums stats into a week overview. Partitioned are started in an int[] {startingIndex, endingIndex}
     */
    public void init(){
        if(!inited && rides.size() >= 1) {
            long timeBetween = System.currentTimeMillis() - rides.get(rides.size() - 1).getDate().getTime();
            totalDays = TimeUnit.DAYS.convert(timeBetween, TimeUnit.MILLISECONDS);
            numWeeks = (int) Math.ceil(totalDays / 7.f);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date()); //sets dat to now
            latestDayOfWeek = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7; //convert Sunday (1) to Saturday (7) to Monday (0) to Sunday (6)
            String lastMonth = new SimpleDateFormat("MMM", Locale.getDefault()).format(now.getTime());
            now.setTime(rides.get(rides.size() - 1).getDate());
            firstDayOfWeek = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            for(int i = 0; i < numWeeks; ++i){
                int[] indexRange = getActivityWeek(i);
                RideOverview overview = new RideOverview();
                for(int k = indexRange[0]; k <= indexRange[1]; ++k){
                    overview.setDistance(overview.getDistance() + rides.get(k).getDistance());
                    overview.setMovingTime(overview.getMovingTime() + rides.get(k).getMovingTime());
                    overview.setTotalTime(overview.getTotalTime() + rides.get(k).getTotalTime());
                    overview.setClimbed(overview.getClimbed() + rides.get(k).getClimbed());
                    overview.setAvgSpeed(overview.getAverageSpeed() + rides.get(k).getAverageSpeed());
                    overview.setPower(overview.getPower() + rides.get(k).getPower());
                }
                long day = (System.currentTimeMillis() - (latestDayOfWeek * (1000 * 3600 * 24))) - (long)i * 1000 * 3600 * 24 * 7; //epoch time of start date
                Date startDate = new Date(day);
                overview.setDate(startDate);
                overview.setName(ctx.getResources().getString(R.string.week_title) + " " + new SimpleDateFormat("MMM dd yyyy", Locale.getDefault()).format(startDate));
                overview.setPower(overview.getPower() / (indexRange[1] - indexRange[0]));
                overview.setAvgSpeed(overview.getAverageSpeed() / (indexRange[1] - indexRange[0]));
                if(filter == null || overview.doesApply(filter)){
                    weekOverview.add(overview);
                    weekIndexes.add(indexRange);
                }

            }
            notifyItemRangeInserted(0, weekOverview.size());
            inited = true;
        }
    }
    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.week_view, parent, false);
        return new WeekViewHolder(v);
    }

    /**
     * Adds all rides in between the range index into the view holder
     * Iterates day by day in a week. For each day, adds all activities that occur on that day to the view holder
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        RideOverview overview = weekOverview.get(position);
        int[] range = weekIndexes.get(position);
        ((TextView)holder.v.findViewById(R.id.weekDate)).setText(overview.getName());
        for(int i = 0, j = range[1]; i < 7 && j >= range[0]; ++i){
            long dayToAdd = overview.getDate().getTime() + i * 1000 * 3600 * 24;
            View view = holder.days[i];
            if(RideOverview.isSameDay(rides.get(j).getDate(), dayToAdd)){
                ((TextView)view.findViewById(R.id.dayTime)).setText(TimeSpan.fromSeconds(rides.get(j).getMovingTime()));
                ((TextView)view.findViewById(R.id.dayDistance)).setText(String.format(Locale.getDefault(), "%.2f %s", rides.get(j).getDistance() * Settings.metersDistanceConversion(), Settings.distanceUnits()));
                int k = j;
                ArrayList<RideOverview> duplications = new ArrayList<>();
                duplications.add(rides.get(j));
                while(k > 0 && RideOverview.isSameDay(rides.get(--k).getDate(), dayToAdd)) duplications.add(rides.get(k));
                final int clickListenerId = j;
                --j;

                //color coding based on exertion and race
                boolean isRace = false;
                int exertion = 0;
                for(RideOverview r : duplications) {
                    isRace = r.getRace() || isRace;
                    exertion = Math.max(exertion, r.getExertion());
                }
                if(isRace){
                    ((TextView)view.findViewById(R.id.dayName)).setTextColor(Util.rgba(1, 0, 0, 0.8f));
                    ((TextView)view.findViewById(R.id.dayDistance)).setTextColor(Util.rgba(1, 0, 0, 0.8f));
                }
                if(exertion > 0)
                    ((CardView)view.findViewById(R.id.weekItemCard)).setCardBackgroundColor(exertion <= 5 ? Util.rgba(exertion / 5.f, 1.f, 0, 1) : Util.rgba(1.0f, (10 - exertion) / 4.f, 0, 1));

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(duplications.size() > 1) {
                            duplications.add(rides.get(clickListenerId));
                            for (Observer o : observers) {
                                o.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_MULTIPLE_NOTIFY, duplications, view));
                            }
                        }else{
                            for (Observer o : observers) {
                                o.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_NOTIFY, rides.get(clickListenerId), view));
                            }
                        }

                    }
                });
            }else if(rides.get(j).getDate().after(new Date(dayToAdd))){ //more days that rides, added all rides that occured on previous days already and next ride occurs on a day we didn't iterate up to yet. So no activity on this day
                ((TextView)view.findViewById(R.id.dayDistance)).setText(ctx.getResources().getString(R.string.rest));
                ((TextView)view.findViewById(R.id.dayTime)).setText("");
            }


        }
        if(range[1] == -1){ //no rides in week
            for(int i = 0; i < 7; ++i){
                holder.days[i].setOnClickListener(null);
                ((TextView)holder.days[i].findViewById(R.id.dayDistance)).setText("");
                ((TextView)holder.days[i].findViewById(R.id.dayTime)).setText("");
            }
        }
        ((TextView)holder.v.findViewById(R.id.weekTime)).setText(TimeSpan.fromSeconds(weekOverview.get(position).getMovingTime()));
        ((TextView)holder.v.findViewById(R.id.weekDistance)).setText(String.format(Locale.getDefault(), "%.2f %s", weekOverview.get(position).getDistance() * Settings.metersDistanceConversion(), Settings.distanceUnits()));
        ((TextView)holder.v.findViewById(R.id.weekElevation)).setText(String.format(Locale.getDefault(), "%.2f %s", weekOverview.get(position).getClimbed() * Settings.metersElevationConversion(), Settings.elevationUnits()));
    }
    @Override
    public void onAttachedToRecyclerView(RecyclerView view){
        super.onAttachedToRecyclerView(view);
        if(view.getLayoutManager() instanceof LinearLayoutManager){
            LinearLayoutManager manager = (LinearLayoutManager)view.getLayoutManager();
            view.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int visiblePos = manager.findFirstVisibleItemPosition();
                    if(visiblePos > -1){
                        scrollListener.onWeekScroll(weekOverview.get(visiblePos));
                    }
                }
            });
        }
    }
    public void setWeekScrollListener(WeekScrollListener onScroll){
        scrollListener = onScroll;
    }

    @Override
    public int getItemCount() {
        return weekOverview.size();
    }

    /**
     * Gets all the activities that occurred in the week relative to today. Returns indexes to be used to index the rideList list
     * Binary searches through rideList based on date
     * @param weekFromToday 0 indexed number identifying the week from today to fetch
     * @return range of activity indexes ordered most recent to earlier (Sun to Mon). Both values are inclusive
     */
    private int[] getActivityWeek(int weekFromToday){
        int[] weekRange = new int[]{0, -1};
        //makes sure startDate is always a monday and endDate is always a sunday to align weeks
        Date endDate = new Date((System.currentTimeMillis() + (6 - latestDayOfWeek) * (long)(1000 * 3600 * 24)) - (long)weekFromToday * 1000 * 3600 * 24 * 7); //first term casts to long bc of System.currentTimeMillis(), second term of subtraction does not implicitly
        Date startDate = new Date((System.currentTimeMillis() - (latestDayOfWeek * (long)(1000 * 3600 * 24))) - (long)weekFromToday * 1000 * 3600 * 24 * 7);
        long key = (endDate.getTime() + startDate.getTime()) / 2;
        int id = 0;
        int start = 0, end = rides.size() - 1; //start and end in memory not time
        //items towards end of list is later

        //interpolated binary search based on date
        do {
            id = (int) Math.round((rides.get(start).getDate().getTime() - key) / (double) (rides.get(start).getDate().getTime() - rides.get(end).getDate().getTime()) * (end - start) + start);
            if(id < start) id = start;
            else if(id >= end) id = end;
            if((rides.get(id).getDate().after(startDate) || RideOverview.isSameDay(rides.get(id).getDate(), startDate)) &&
                    (rides.get(id).getDate().before(endDate) || RideOverview.isSameDay(rides.get(id).getDate(), endDate))){
                weekRange = new int[]{id, id};
                int index = id + 1;
                while(index < rides.size() && (rides.get(index).getDate().after(startDate) || RideOverview.isSameDay(rides.get(index).getDate(), startDate))){
                    weekRange[1] = index++;
                }
                index = id - 1;
                while(index >= 0 && (rides.get(index).getDate().before(endDate) || RideOverview.isSameDay(rides.get(index).getDate(), endDate))){
                    weekRange[0] = index--;
                }
                break;
            }
            else if(rides.get(id).getDate().before(startDate)){
                end = id - 1;
            }else if(rides.get(id).getDate().after(endDate)){
                start = id + 1;
            }
        } while(start <= end && start < rides.size() && end < rides.size());
        return weekRange;
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
}
interface WeekScrollListener {
    public void onWeekScroll(RideOverview firstVisibleWeekOverview);
}
