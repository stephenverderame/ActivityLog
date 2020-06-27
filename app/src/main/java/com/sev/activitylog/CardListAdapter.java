package com.sev.activitylog;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.LinkedList;

public class CardListAdapter extends RecyclerView.Adapter<CardListAdapter.CardListViewHolder> implements Subject {
    private LinkedList<RideOverview> data;
    private LinkedList<Observer> observers;
    public CardListAdapter(LinkedList<RideOverview> data){
        this.data = data;
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
