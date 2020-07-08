package com.sev.activitylog;

import android.app.DatePickerDialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FilterView extends FrameLayout implements View.OnClickListener {
    private boolean isShowingAdvanced;
    private Button advancedButton;
    private onSearchListener searchListener;
    private Future<ArrayList<Gear>> gearList;
    public FilterView(Context context) {
        super(context);
        init(context);
    }

    public FilterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FilterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public FilterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }
    public void setGearList(Future<ArrayList<Gear>> g){ gearList = g;}

    private void init(Context ctx){
        inflate(ctx, R.layout.filter_view, this);
        isShowingAdvanced = false;
        advancedButton = (Button)findViewById(R.id.advanced_filter_btn);
        advancedButton.setOnClickListener(this);
        findViewById(R.id.start_date_filter).setOnClickListener(this);
        findViewById(R.id.search_filter_btn).setOnClickListener(this);
    }
    private void setGearList(ArrayList<Gear> gears){
        String[] gearNames = new String[gears.size()];
        for(int i = 0; i < gears.size(); ++i)
            gearNames[i] = gears.get(i).gearName;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, gearNames);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        ((Spinner)findViewById(R.id.gear_select_filter)).setAdapter(adapter);
    }
    public void setOnSearchListener(onSearchListener l){
        searchListener = l;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.advanced_filter_btn:
                advancedButton.setText(isShowingAdvanced ? "Advanced" : "Hide");
                findViewById(R.id.advanced_filters_container).setVisibility(isShowingAdvanced ? GONE : VISIBLE);
                isShowingAdvanced = !isShowingAdvanced;
                if(isShowingAdvanced && gearList != null && gearList.isDone()){
                    try {
                        setGearList(gearList.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.start_date_filter:
                showDateDialog(R.id.start_date_filter);
                break;
            case R.id.end_date_filter:
                showDateDialog(R.id.end_date_filter);
                break;
            case R.id.search_filter_btn:
            {
                FilterBuilder builder = new FilterBuilder();
                EditText entry = (EditText)findViewById(R.id.filter_search);
                if(entry.getText().toString().length() >= 1) builder.name(entry.getText().toString());
                if(findViewById(R.id.advanced_filters_container).getVisibility() == VISIBLE) {
                    entry = (EditText) findViewById(R.id.start_date_filter);
                    if (entry.getText().toString().length() >= 1) {
                        try {
                            builder.startDate(new SimpleDateFormat("MM/dd/yyyy").parse(entry.getText().toString()));
                            entry = (EditText) findViewById(R.id.end_date_filter);
                            if(entry.getText().toString().length() >= 1) builder.endDate(new SimpleDateFormat("MM/dd/yyyy").parse(entry.getText().toString()));
                            entry = (EditText) findViewById(R.id.time_min_filter);
                            if(entry.getText().toString().length() >= 1) builder.minTime((int) (new SimpleDateFormat("HH:mm:ss").parse(entry.getText().toString()).getTime() / 1000));
                            entry = (EditText) findViewById(R.id.time_max_filter);
                            if(entry.getText().toString().length() >= 1) builder.maxTime((int) (new SimpleDateFormat("HH:mm:ss").parse(entry.getText().toString()).getTime() / 1000));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    Spinner spin = (Spinner) findViewById(R.id.gear_select_filter);
                    if (spin.getSelectedItem().toString() != "All")
                        builder.gear(spin.getSelectedItem().toString());
                    spin = (Spinner) findViewById(R.id.activity_select_filter);
                    if (spin.getSelectedItem().toString() != "All")
                        builder.workoutType(spin.getSelectedItem().toString());

                    entry = (EditText) findViewById(R.id.distance_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minDist(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.distance_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxDist(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.speed_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minSpeed(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.speed_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxSpeed(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.elevation_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minElevation(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.elevation_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxElevation(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.power_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minPow(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) findViewById(R.id.power_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxPow(Float.parseFloat(entry.getText().toString()));
                }
                if(searchListener != null) searchListener.search(builder.build());
                break;
            }
        }
    }
    private void showDateDialog(int editTextId){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    ((EditText)findViewById(editTextId)).setText((month + 1) + "/" + day + "/" + year);
                }
            }, 2020, 1, 1);
            dialog.show();
        }
    }
}
class SearchFilters{
    public enum Bool{
        True, False, Ignore
    }
    public Date start, end;
    public int minTime = -1, maxTime = -1;
    public String gear, workoutType, raceType, name;
    public Bool trainer = Bool.Ignore;
    public float minSpeed = -1, maxSpeed = -1, minPow = -1, maxPow = -1, minElevation = -1, maxElevation = -1, minDist = -1, maxDist = -1;
    public boolean isDefaultValue(Object parameter){
        if(parameter instanceof Date || parameter instanceof String)
            return parameter == null;
        else if(parameter instanceof Bool)
            return parameter == Bool.Ignore;
        else if(parameter instanceof Float)
            return (Float)parameter == -1;
        else if(parameter instanceof Integer)
            return (Integer)parameter == -1;
        throw new IllegalArgumentException("Parameter not a type of a filter field!");
    }
}
class FilterBuilder {
    private SearchFilters filters;
    public FilterBuilder(){filters = new SearchFilters();}
    public SearchFilters build() {return filters;}
    public FilterBuilder startDate(Date start){
        filters.start = start;
        return this;
    }
    public FilterBuilder endDate(Date end){
        filters.end = end;
        return this;
    }
    public FilterBuilder minTime(int t){
        filters.minTime = t;
        return this;
    }
    public FilterBuilder maxTime(int t){
        filters.maxTime = t;
        return this;
    }
    public FilterBuilder minDist(float t){
        filters.minDist = t;
        return this;
    }
    public FilterBuilder maxDist(float t){
        filters.maxDist = t;
        return this;
    }
    public FilterBuilder minElevation(float t){
        filters.minElevation = t;
        return this;
    }
    public FilterBuilder maxElevation(float t){
        filters.maxElevation = t;
        return this;
    }
    public FilterBuilder minSpeed(float t){
        filters.minSpeed = t;
        return this;
    }
    public FilterBuilder maxSpeed(float t){
        filters.maxSpeed = t;
        return this;
    }
    public FilterBuilder minPow(float t){
        filters.minPow = t;
        return this;
    }
    public FilterBuilder maxPow(float t){
        filters.maxPow = t;
        return this;
    }
    public FilterBuilder name(String t){
        filters.name = t;
        return this;
    }
    public FilterBuilder gear(String t){
        filters.gear = t;
        return this;
    }
    public FilterBuilder workoutType(String t){
        filters.workoutType = t;
        return this;
    }
    public FilterBuilder raceType(String t){
        filters.raceType = t;
        return this;
    }
    public FilterBuilder trainer(SearchFilters.Bool t){
        filters.trainer = t;
        return this;
    }
}
interface onSearchListener {
    public void search(SearchFilters filter);
}
