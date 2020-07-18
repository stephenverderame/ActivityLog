package com.sev.activitylog;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class TrendsFragment extends DataFragment implements View.OnClickListener {
    private View view;
    private GraphView graph;
    private ArrayList<ArrayList<Pair<Double, Double>>> dataSet;
    private List<RideOverview> filteredData;
    double dataMinY, dataMaxY;
    public TrendsFragment(List<RideOverview> rideList, Observer controller) {
        super(rideList, controller);
        filteredData = data;
    }

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
            if(dataSet != null){
                graph.setData(dataSet.get(0), 0);
                graph.setScale(new GraphScale(data.get(0).getDate().getTime(), data.get(data.size() - 1).getDate().getTime(), (float) dataMaxY, (float) dataMinY));
                graph.setStyle(new GraphStyleBuilder().colors(Color.CYAN).strokes(2).x("Rides").y("Speed").title("Speed vs Time").grid(true)
                        .type(GraphStyle.GraphType.TYPE_DATE_LINE).regresssionStyle(1, Util.rgba(1, 0, 0, .5f)).regression(true).build());
            }
        }
        return view;
    }
    @Override
    public void filter(SearchFilters filter) {
        filteredData = commonFilter(filter);
    }

    @Override
    public void notifyDataInsertion(int index, int count) {

    }

    @Override
    public void notifyDataChanged(int pos, int count) {

    }

    @Override
    public void notifyDataFinish() {
        dataSet = new ArrayList<>();
        double minSpeed = 100, maxSpeed = 0;
        dataSet.add(new ArrayList<Pair<Double, Double>>());
        for(int i = data.size() - 1; i >= 0; --i) {
            RideOverview r = data.get(i);
            dataSet.get(0).add(new Pair<Double, Double>((double) r.getDate().getTime(), (double) r.getAverageSpeed() * RideOverview.METERS_MILES_CONVERSION * 3600));
            if(r.getAverageSpeed() * RideOverview.METERS_MILES_CONVERSION * 3600 < minSpeed) minSpeed = r.getAverageSpeed() * RideOverview.METERS_MILES_CONVERSION * 3600;
            if(r.getAverageSpeed() * RideOverview.METERS_MILES_CONVERSION * 3600 > maxSpeed) maxSpeed = r.getAverageSpeed() * RideOverview.METERS_MILES_CONVERSION * 3600;
        }
        dataMinY = minSpeed;
        dataMaxY = maxSpeed;
        if(graph != null) {
            graph.setData(dataSet.get(0), 0);
            graph.setScale(new GraphScale(data.get(0).getDate().getTime(), data.get(data.size() - 1).getDate().getTime(), (float) maxSpeed, (float) minSpeed));
            graph.setStyle(new GraphStyleBuilder().colors(Color.CYAN).strokes(2).x("Rides").y("Speed").title("Speed vs Time").grid(true).type(GraphStyle.GraphType.TYPE_DATE_LINE).build());
            graph.invalidate();
        }
    }

    @Override
    public void notifyDataChanged() {

    }

    @Override
    public void loadStateFromParcel(Parcelable state) {

    }

    @Override
    public Parcelable getState() {
        return null;
    }

    @Override
    public void cacheState() {

    }

    //graphButtonClick
    @Override
    public void onClick(View btn) {
        FunctionalSpinnerItem x = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphXAxis)).getSelectedItem();
        FunctionalSpinnerItem y = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphYAxis)).getSelectedItem();
        FunctionalSpinnerItem interval = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphIntervalSelector)).getSelectedItem();
        String intervalNumStr = ((EditText)view.findViewById(R.id.graphIntervalNum)).getText().toString();
        String lineNumStr = ((EditText)view.findViewById(R.id.graphGroupNum)).getText().toString();
        FunctionalSpinnerItem groupInterval = (FunctionalSpinnerItem)((Spinner)view.findViewById(R.id.graphGroupIntervalSelector)).getSelectedItem();
        int numIntervals = intervalNumStr.length() >= 1 ? Integer.parseInt(intervalNumStr) : 1;
        int numGroups = lineNumStr.length() >= 1 ? Integer.parseInt(lineNumStr) : 1;
        boolean reg = ((CheckBox)view.findViewById(R.id.graphRegression)).isChecked();

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
        long groupIntervalTime = Util.intervalSeconds(numGroups, groupInterval);
        intervals = 1;
        dataSet.clear();
        dataSet.add(new ArrayList<>());
        GraphStyleBuilder styler = new GraphStyleBuilder(graph.getStyle());
        dataSet.get(0).add(new Pair<>(Util.getStat(intervalRides.get(0), x), Util.getStat(intervalRides.get(0), y)));
        for(int i = 1; i < intervalRides.size(); ++i){
            if(start - i * intervalTime > start - intervals * groupIntervalTime){
                dataSet.get(intervals - 1).add(new Pair<>(Util.getStat(intervalRides.get(i), x), Util.getStat(intervalRides.get(i), y)));
            }else{
                graph.setData(dataSet.get(intervals - 1), intervals - 1);
                styler.color(Util.rgba((float)Math.sin(System.currentTimeMillis()) * 0.5f + 1, (float)Math.cos(System.currentTimeMillis()) * 0.5f + 1,
                        (float)Math.sin(1.004 * System.currentTimeMillis()) * 0.5f + 1, 1), intervals - 1);
                styler.stroke(2, intervals - 1);
                dataSet.add(new ArrayList<>());
                dataSet.get(intervals++).add(new Pair<>(Util.getStat(intervalRides.get(i), x), Util.getStat(intervalRides.get(i), y)));
            }
        }
        graph.setData(dataSet.get(intervals - 1), intervals - 1);
        styler.x(x.getText(getResources())).y(y.getText(getResources())).title(y.getText(getResources()) + " vs " + x.getText(getResources())).regression(reg);
        if(x.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITIES))
            styler.type(GraphStyle.GraphType.TYPE_DATE_LINE);
        else if(x.equals(FunctionalSpinnerItem.SPIN_STAT_MV_TIME) || x.equals(FunctionalSpinnerItem.SPIN_STAT_TL_TIME))
            styler.type(GraphStyle.GraphType.TYPE_TIME_LINE);
        else
            styler.type(GraphStyle.GraphType.TYPE_LINE);
        if(x.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITY_TYPE))
            styler.addFlag(GraphStyle.STRING_AS_X);
        if(reg) styler.regression(true).addFlag(GraphStyle.MATCH_REG_TO_LINE).grid(true);
        graph.setStyle(styler.build());
        graph.setScale(null); //auto compute scale
        graph.invalidate();

    }
}
