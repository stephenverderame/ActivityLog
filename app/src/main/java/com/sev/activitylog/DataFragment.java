package com.sev.activitylog;

import android.os.Parcelable;

import androidx.fragment.app.Fragment;

import java.util.LinkedList;
import java.util.List;

public abstract class DataFragment extends Fragment {
    protected Observer mediator;
    protected List<RideOverview> data;
    public DataFragment(List<RideOverview> d, Observer controller){
        data = d;
        mediator = controller;
    }
    public DataFragment(){}
    public abstract void filter(SearchFilters filter);
    public abstract void notifyDataInsertion(int index, int count);
    public abstract void notifyDataChanged(int pos, int count);
    public abstract void notifyDataFinish();
    public abstract void notifyDataChanged();
    public abstract void loadStateFromParcel(Parcelable state);
    public abstract Parcelable getState();
    public void setData(List<RideOverview> data){
        this.data = data;
        notifyDataChanged();
    }
    //Saves current state internally to be internally reloaded by the same instance
    public abstract void cacheState();

    protected LinkedList<RideOverview> commonFilter(SearchFilters filter){
        LinkedList<RideOverview> rides = new LinkedList<>();
        int start = filter.isDefaultValue(filter.start) ? data.size() - 1 : RideOverview.indexOf(data, filter.start);
        int end = filter.isDefaultValue(filter.end) ? 0 : RideOverview.indexOf(data, filter.end);
        for (; end <= start; ++end) {
            if (data.get(end).doesApply(filter)) rides.add(data.get(end));
        }
        return rides;
    }
}
