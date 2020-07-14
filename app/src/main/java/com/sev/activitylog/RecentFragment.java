package com.sev.activitylog;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;
public class RecentFragment extends DataFragment {
    private RecyclerView view;
    private RecentActivityAdapter viewAdapter;
    private Parcelable lastState;
    private LinkedList<RideOverview> rides;
    public RecentFragment(List<RideOverview> data, Observer controller){
        super(data, controller);
    }

    @Override
    public void filter(SearchFilters filter) {
        rides = new LinkedList<>();
        int start = filter.isDefaultValue(filter.start) ? data.size() - 1 : RideOverview.indexOf(data, filter.start);
        int end = filter.isDefaultValue(filter.end) ? 0 : RideOverview.indexOf(data, filter.end);
        for (; end <= start; ++end) {
            if (data.get(end).doesApply(filter)) rides.add(data.get(end));
        }
        viewAdapter = new RecentActivityAdapter(rides, mediator);
        view.setAdapter(viewAdapter);
    }

    @Override
    public void notifyDataInsertion(int index, int count) {
        if(viewAdapter != null) viewAdapter.notifyItemRangeInserted(index, count);
    }

    @Override
    public void notifyDataChanged(int pos, int count) {
        if(viewAdapter != null) viewAdapter.notifyItemRangeChanged(pos, count);
    }


    @Override
    public void notifyDataFinish() {
        if(viewAdapter != null) viewAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataChanged() {
        if(viewAdapter != null) viewAdapter.notifyDataSetChanged();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = new RecyclerView(new ContextThemeWrapper(getContext(), R.style.ScrollbarRecyclerView));
            view.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        if(viewAdapter == null) {
            viewAdapter = new RecentActivityAdapter(rides == null ? data : rides);
            viewAdapter.attach(mediator);
        }
        view.setAdapter(viewAdapter);
        if(lastState != null){
            view.getLayoutManager().onRestoreInstanceState(lastState);
            lastState = null;
        }
        return view;
    }
    @Override
    public void loadStateFromParcel(Parcelable state){
        RecentMemento s = (RecentMemento)state;
        if(view != null) {
            viewAdapter = new RecentActivityAdapter(s.getAdapterData(), mediator);
            view.setAdapter(viewAdapter);
            view.getLayoutManager().onRestoreInstanceState(s.getViewState());
        }
        else{
            lastState = s.getViewState();
            rides = s.getAdapterData();
        }
    }
    @Override
    public Parcelable getState(){
        return new RecentMemento(view == null ? null : view.getLayoutManager().onSaveInstanceState(), rides);
    }

    @Override
    public void cacheState() {
        if(view != null) lastState = view.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void setData(List<RideOverview> data) {
        rides = new LinkedList<>();
        rides.addAll(data);
    }
}
class RecentMemento implements Parcelable {
    private Parcelable viewState;
    private LinkedList<RideOverview> adapterData;

    public RecentMemento(Parcel in) {
        viewState = in.readParcelable(RecyclerView.LayoutManager.class.getClassLoader());
        adapterData = (LinkedList<RideOverview>)in.readSerializable();
    }
    public RecentMemento(Parcelable viewState, LinkedList<RideOverview> adapterData){
        this.viewState = viewState;
        this.adapterData = adapterData;
    }

    public static final Creator<RecentMemento> CREATOR = new Creator<RecentMemento>() {
        @Override
        public RecentMemento createFromParcel(Parcel in) {
            return new RecentMemento(in);
        }

        @Override
        public RecentMemento[] newArray(int size) {
            return new RecentMemento[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(viewState, 0);
        parcel.writeSerializable(adapterData);
    }
    public Parcelable getViewState(){
        return viewState;
    }
    public LinkedList<RideOverview> getAdapterData(){
        return adapterData;
    }
}

