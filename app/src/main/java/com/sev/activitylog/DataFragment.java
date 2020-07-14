package com.sev.activitylog;

import android.os.Parcelable;

import androidx.fragment.app.Fragment;

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
    public abstract void cacheState();
}
