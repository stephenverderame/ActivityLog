package com.sev.activitylog;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

public class WeekFragment extends DataFragment {
    private RecyclerView view;
    private WeekViewAdapter viewAdapter;
    private boolean finished = false;
    private Parcelable lastState;
    private SearchFilters sf;
    public WeekFragment(List<RideOverview> data, Observer controller){
        super(data, controller);
    }

    @Override
    public void filter(SearchFilters filter) {
        sf = filter;
        viewAdapter = new WeekViewAdapter(data, filter, getContext(), mediator);
        viewAdapter.init();
        view.setAdapter(viewAdapter); //when the view is updated. OnCreateView is called again
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
        finished = true;
        notifyDataChanged();
    }

    @Override
    public void notifyDataChanged() {
        if(viewAdapter != null){
            viewAdapter.init();
            viewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadStateFromParcel(Parcelable state) {
        WeekMemento wm = (WeekMemento)state;
        if(view != null)
            view.getLayoutManager().onRestoreInstanceState(wm.getViewState());
        else
            lastState = wm.getViewState();
        sf = wm.getFilter();
    }

    @Override
    public Parcelable getState() {
        return new WeekMemento(view != null ? view.getLayoutManager().onSaveInstanceState() : null, sf);
    }

    @Override
    public void cacheState() {
        if(view != null) lastState = view.getLayoutManager().onSaveInstanceState();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = new RecyclerView(getContext());
            view.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        if(viewAdapter == null) {
            viewAdapter = sf == null ? new WeekViewAdapter(data, getContext()) : new WeekViewAdapter(data, sf, getContext());
            if(data.size() > 1) viewAdapter.init();
            viewAdapter.attach(mediator);
        }
        if(finished || lastState != null) viewAdapter.init();
        view.setAdapter(viewAdapter);
        if(lastState != null){
            view.getLayoutManager().onRestoreInstanceState(lastState);
            lastState = null;
        }
        return view;
    }
}
class WeekMemento implements Parcelable {
    private Parcelable viewState;
    private SearchFilters filter;

    public WeekMemento(Parcel in) {
        viewState = in.readParcelable(RecyclerView.LayoutManager.class.getClassLoader());
        filter = (SearchFilters)in.readSerializable();
    }
    public WeekMemento(Parcelable viewState, SearchFilters filter){
        this.viewState = viewState;
        this.filter = filter;
    }

    public static final Creator<WeekMemento> CREATOR = new Creator<WeekMemento>() {
        @Override
        public WeekMemento createFromParcel(Parcel in) {
            return new WeekMemento(in);
        }

        @Override
        public WeekMemento[] newArray(int size) {
            return new WeekMemento[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(viewState, 0);
        parcel.writeSerializable(filter);
    }
    public SearchFilters getFilter() {return filter;}
    public Parcelable getViewState() {return viewState;}
}