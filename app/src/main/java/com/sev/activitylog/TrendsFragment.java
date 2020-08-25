package com.sev.activitylog;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TrendsFragment extends DataFragment implements View.OnClickListener {
    private View view;
    private GraphView graph;
    private GraphStyle style;
    private ArrayList<ArrayList<Tuple<Double, Double>>> dataSet;
    private List<RideOverview> filteredData;
    double dataMinY, dataMaxY;
    public TrendsFragment(List<RideOverview> rideList, Observer controller) {
        super(rideList, controller);
        filteredData = data;
    }
    public TrendsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.trends_layout, null);
            view.findViewById(R.id.graphBtn).setOnClickListener(this);
            ((Spinner)view.findViewById(R.id.graphIntervalSelector)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_INT_LIST));
            ((Spinner)view.findViewById(R.id.graphGroupIntervalSelector)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_INT_LIST));
            ((Spinner)view.findViewById(R.id.graphXAxis)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_STAT_LIST));
            ((Spinner)view.findViewById(R.id.graphYAxis)).setAdapter(new FunctionalSpinAdapter(getContext(), android.R.layout.simple_spinner_item, FunctionalSpinnerItem.SPIN_STAT_LIST));
            graph = (GraphView)view.findViewById(R.id.graph);
            if(dataSet == null && filteredData != null && filteredData.size() > 0){
                dataSet = new ArrayList<>();
                dataSet.add(new ArrayList<Tuple<Double, Double>>());
                for(int i = data.size() - 1; i >= 0; --i) {
                    RideOverview r = data.get(i);
                    dataSet.get(0).add(new Tuple<Double, Double>((double) r.getDate().getTime(), (double) r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600));
                }
            }
            if(dataSet != null){
                int i = 0;
                for(ArrayList<Tuple<Double, Double>> list : dataSet)
                    graph.setData(list, i++);
                graph.setScale(null); //auto-detect scale
                graph.setStyle(style != null ? style : new GraphStyleBuilder().colors(Color.CYAN).strokes(2).x("Rides").y("Speed").title("Speed vs Time").grid(true)
                        .type(GraphStyle.GraphType.TYPE_LINE).regresssionStyle(1, Util.rgba(1, 0, 0, .5f)).regression(true).build());
                graph.invalidate();
            }
        }
        return view;
    }
    @Override
    public void filter(SearchFilters filter) {
        filteredData = commonFilter(filter);
        Toast.makeText(getContext(), getResources().getString(R.string.filter_confirmation), Toast.LENGTH_LONG).show();
    }

    @Override
    public void notifyDataInsertion(int index, int count) {

    }

    @Override
    public void notifyDataChanged(int pos, int count) {

    }

    @Override
    public void notifyDataFinish() {
        if(dataSet == null || graph == null) {
            dataSet = new ArrayList<>();
            double minSpeed = 100, maxSpeed = 0;
            dataSet.add(new ArrayList<Tuple<Double, Double>>());
            for (int i = data.size() - 1; i >= 0; --i) {
                RideOverview r = data.get(i);
                dataSet.get(0).add(new Tuple<Double, Double>((double) r.getDate().getTime(), (double) r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600));
                if (r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600 < minSpeed)
                    minSpeed = r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600;
                if (r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600 > maxSpeed)
                    maxSpeed = r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600;
            }

            dataMinY = minSpeed;
            dataMaxY = maxSpeed;
            if (graph != null && data != null) {
                graph.setData(dataSet.get(0), 0);
                graph.setScale(new GraphScale(data.get(0).getDate().getTime(), data.get(data.size() - 1).getDate().getTime(), (float) maxSpeed, (float) minSpeed));
                graph.setStyle(new GraphStyleBuilder().colors(Color.CYAN).strokes(2).x("Rides").y("Speed").title("Speed vs Time").grid(true).type(GraphStyle.GraphType.TYPE_LINE).build());
                graph.invalidate();
            }
        }else{
            int i = 0;
            for(ArrayList<Tuple<Double, Double>> list : dataSet)
                graph.setData(list, i++);
            graph.setScale(null); //auto-detect scale
            graph.setStyle(style != null ? style : new GraphStyleBuilder().colors(Color.CYAN).strokes(2).x("Rides").y("Speed").title("Speed vs Time").grid(true)
                    .type(GraphStyle.GraphType.TYPE_LINE).regresssionStyle(1, Util.rgba(1, 0, 0, .5f)).regression(true).build());
            graph.invalidate();
        }
    }

    @Override
    public void notifyDataChanged() {

    }

    @Override
    public void loadStateFromParcel(Parcelable state) {
        TrendsMemento memento = (TrendsMemento)state;
        dataSet = memento.getData();
        style = memento.getStyle();
        if(graph != null){
            graph.clearData();
            int i = 0;
            for(ArrayList<Tuple<Double, Double>> p : dataSet)
                graph.setData(p, i++);
            graph.setStyle(style);
        }
    }

    @Override
    public Parcelable getState() {
        return new TrendsMemento(style, dataSet);
    }

    @Override
    public void cacheState() { //done automatically
    }

    @Override
    public void setData(List<RideOverview> data) {
        super.setData(data);
        filteredData = data;
    }

    //graphButtonClick
    @Override
    public void onClick(View btn) {
        if(filteredData.size() == 0) return;
        graph.clearData();
        FunctionalSpinnerItem x = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphXAxis)).getSelectedItem();
        FunctionalSpinnerItem y = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphYAxis)).getSelectedItem();
        FunctionalSpinnerItem interval = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphIntervalSelector)).getSelectedItem();
        String intervalNumStr = ((EditText)view.findViewById(R.id.graphIntervalNum)).getText().toString();
        String lineNumStr = ((EditText)view.findViewById(R.id.graphGroupNum)).getText().toString();
        FunctionalSpinnerItem groupInterval = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphGroupIntervalSelector)).getSelectedItem();
        int numIntervals = intervalNumStr.length() >= 1 ? Integer.parseInt(intervalNumStr) : 1;
        int numGroups = lineNumStr.length() >= 1 ? Integer.parseInt(lineNumStr) : 1;
        boolean reg = ((CheckBox)view.findViewById(R.id.graphRegression)).isChecked();
        int type = ((Spinner)view.findViewById(R.id.graphType)).getSelectedItemPosition();

        ArrayList<MasterRideOverview> intervalRides = new ArrayList<>();
        long start = filteredData.get(0).getDate().getTime();
        long intervalTime = Util.intervalSeconds(numIntervals, interval) * 1000L;
        int intervals = 1;
        MasterRideOverview master = new MasterRideOverview();
        master.addRideData(filteredData.get(0));
        for(int i = 1; i < filteredData.size(); ++i){
            if(filteredData.get(i).getDate().getTime() > start - intervals * intervalTime){
                master.addRideData(filteredData.get(i));
            }else{
                intervalRides.add(master);
                master = new MasterRideOverview();
                master.addRideData(filteredData.get(i));
                ++intervals;
            }
        }
        intervalRides.add(master);
        if(groupInterval.equals(FunctionalSpinnerItem.SPIN_INTERVAL_ACTIVITY)) groupInterval = FunctionalSpinnerItem.SPIN_INTERVAL_NONE;
        long groupIntervalTime = Util.intervalSeconds(numGroups, groupInterval) * 1000L;
        intervals = 1;
        dataSet.clear();
        dataSet.add(new ArrayList<>());
        GraphStyleBuilder styler = new GraphStyleBuilder();
        dataSet.get(0).add(new Tuple<>(Util.getStat(intervalRides.get(0), x), Util.getStat(intervalRides.get(0), y)));
        for(int i = 1; i < intervalRides.size(); ++i){
            if(intervalRides.get(i).getDate().getTime() > start - intervals * groupIntervalTime){
                dataSet.get(intervals - 1).add(new Tuple<>(Util.getStat(intervalRides.get(i), x), Util.getStat(intervalRides.get(i), y)));
            }else{
                graph.setData(dataSet.get(intervals - 1), intervals - 1);
                styler.color(Util.rgba((float)Math.sin(System.currentTimeMillis()) * 0.5f + 1, (float)Math.cos(System.currentTimeMillis()) * 0.5f + 1,
                        (float)Math.sin(1.004 * System.currentTimeMillis()) * 0.5f + 1, 1), intervals - 1);
                styler.stroke(2, intervals - 1);
                dataSet.add(new ArrayList<>());
                dataSet.get(intervals++).add(new Tuple<>(Util.getStat(intervalRides.get(i), x), Util.getStat(intervalRides.get(i), y)));
            }
        }
        graph.setData(dataSet.get(intervals - 1), intervals - 1);
        styler.color(Util.rgba((float)Math.sin(Math.random() * System.currentTimeMillis()) * 0.5f + 1, (float)Math.cos(System.currentTimeMillis()) * 0.5f + 1,
                (float)Math.sin(Math.random()) * 0.5f + 1, 1), intervals - 1);
        styler.stroke(2, intervals - 1);
        styler.x(x.getText(getResources())).y(y.getText(getResources())).title(y.getText(getResources()) + " vs " + x.getText(getResources())).regression(reg).format(x, y).grid(true);
        if(reg) styler.regression(true).addFlag(GraphStyle.MATCH_REG_TO_LINE);
        styler.type(type == 0 ? GraphStyle.GraphType.TYPE_LINE : (type == 1 ? GraphStyle.GraphType.TYPE_BAR : GraphStyle.GraphType.TYPE_SCATTER));
        graph.setStyle(styler.build());
        style = styler.build();
        graph.setScale(null); //auto compute scale
        graph.invalidate();

        LinearLayout key = (LinearLayout) view.findViewById(R.id.lineKey);
        key.removeAllViews();
        if(!groupInterval.equals(FunctionalSpinnerItem.SPIN_INTERVAL_NONE)) {
            GraphStyle style = graph.getStyle();
            for (int i = 0; i < style.colors.length; ++i) {
                TextView tv = new TextView(getContext());
                tv.setTextColor(style.colors[i]);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tv.setText(String.format("%d %s", i, groupInterval.getText(getResources())));
                key.addView(tv);
            }
        }

    }
}
class TrendsMemento implements Parcelable {

    private GraphStyle style;
    private ArrayList<ArrayList<Tuple<Double, Double>>> data;

    public TrendsMemento(Parcel in) {
        style = (GraphStyle)in.readSerializable();
        data = (ArrayList<ArrayList<Tuple<Double, Double>>>) in.readSerializable();
    }
    public TrendsMemento(GraphStyle style, ArrayList<ArrayList<Tuple<Double, Double>>> data){
        this.style = style;
        this.data = data;
    }

    public static final Creator<TrendsMemento> CREATOR = new Creator<TrendsMemento>() {
        @Override
        public TrendsMemento createFromParcel(Parcel in) {
            return new TrendsMemento(in);
        }

        @Override
        public TrendsMemento[] newArray(int size) {
            return new TrendsMemento[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(style);
        parcel.writeSerializable(data);
    }

    public GraphStyle getStyle() {return style;}

    public ArrayList<ArrayList<Tuple<Double, Double>>> getData() {
        return data;
    }
}
