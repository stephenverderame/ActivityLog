package com.sev.activitylog;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FilterFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener {
    private boolean isShowingAdvanced;
    private Button advancedButton;
    private onSearchListener searchListener;
    private Future<ArrayList<Gear>> gearList;
    private ArrayList<Gear> loadedGearList;
    private View view;
    public void setGearList(Future<ArrayList<Gear>> g){ gearList = g;}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.filter_view, null);
        init();
        return view;
    }

    private void init(){
        isShowingAdvanced = false;
        advancedButton = (Button)view.findViewById(R.id.advanced_filter_btn);
        advancedButton.setOnClickListener(this);
        view.findViewById(R.id.start_date_filter).setOnFocusChangeListener(this);
        view.findViewById(R.id.end_date_filter).setOnFocusChangeListener(this);
        view.findViewById(R.id.time_min_filter).setOnFocusChangeListener(this);
        view.findViewById(R.id.time_max_filter).setOnFocusChangeListener(this);
        view.findViewById(R.id.start_date_filter).setOnClickListener(this);
        view.findViewById(R.id.end_date_filter).setOnClickListener(this);
        view.findViewById(R.id.time_min_filter).setOnClickListener(this);
        view.findViewById(R.id.time_max_filter).setOnClickListener(this);
        view.findViewById(R.id.search_filter_btn).setOnClickListener(this);
        view.findViewById(R.id.advanced_filters_container).setVisibility(View.GONE);
    }
    private void setGearList(ArrayList<Gear> gears){
        loadedGearList = gears;
        String[] gearNames = new String[gears.size() + 1];
        gearNames[0] = "All";
        for(int i = 0; i < gears.size(); ++i)
            gearNames[i + 1] = gears.get(i).gearName;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, gearNames);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        ((Spinner)view.findViewById(R.id.gear_select_filter)).setAdapter(adapter);
        StorageModel mod = new StorageModel(getContext());
        mod.saveGearList(gears);
    }
    public void setOnSearchListener(onSearchListener l){
        searchListener = l;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.advanced_filter_btn:
                advancedButton.setText(isShowingAdvanced ? "Advanced" : "Hide");
                view.findViewById(R.id.advanced_filters_container).setVisibility(isShowingAdvanced ? View.GONE : View.VISIBLE);
                isShowingAdvanced = !isShowingAdvanced;
                if(!isShowingAdvanced) hideKeyboard();
                if(isShowingAdvanced && gearList != null && gearList.isDone()){
                    try {
                        setGearList(gearList.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.search_filter_btn:
            {
                hideKeyboard();
                FilterBuilder builder = new FilterBuilder();
                EditText entry = (EditText)view.findViewById(R.id.filter_search);
                if(entry.getText().toString().length() >= 1) builder.name(entry.getText().toString());
                if(view.findViewById(R.id.advanced_filters_container).getVisibility() == View.VISIBLE) {
                        try {
                            entry = (EditText) view.findViewById(R.id.start_date_filter);
                            if(entry.getText().toString().length() >= 1) builder.startDate(new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(entry.getText().toString()));
                            entry = (EditText) view.findViewById(R.id.end_date_filter);
                            if(entry.getText().toString().length() >= 1) builder.endDate(new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).parse(entry.getText().toString()));
                            entry = (EditText) view.findViewById(R.id.time_min_filter);
                            if(entry.getText().toString().length() >= 1) builder.minTime((int) (kkmmToInt(entry.getText().toString())));
                            entry = (EditText) view.findViewById(R.id.time_max_filter);
                            if(entry.getText().toString().length() >= 1) builder.maxTime((int) (kkmmToInt(entry.getText().toString())));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    Spinner spin = (Spinner) view.findViewById(R.id.gear_select_filter);
                    if (!spin.getSelectedItem().toString().equalsIgnoreCase("All")) {
                        String itemId = spin.getSelectedItem().toString();
                        if(loadedGearList != null){
                            for(Gear g : loadedGearList){
                                if(g.gearName.equals(itemId)) {
                                    itemId = g.gearId;
                                    break;
                                }
                            }
                        }
                        builder.gear(itemId);
                    }
                    spin = (Spinner) view.findViewById(R.id.activity_select_filter);
                    if (!spin.getSelectedItem().toString().equalsIgnoreCase("All"))
                        builder.workoutType(spin.getSelectedItem().toString());

                    entry = (EditText) view.findViewById(R.id.distance_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minDist(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.distance_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxDist(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.speed_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minSpeed(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.speed_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxSpeed(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.elevation_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minElevation(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.elevation_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxElevation(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.power_min_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.minPow(Float.parseFloat(entry.getText().toString()));
                    entry = (EditText) view.findViewById(R.id.power_max_filter);
                    if (entry.getText().toString().length() >= 1)
                        builder.maxPow(Float.parseFloat(entry.getText().toString()));

                    view.findViewById(R.id.advanced_filters_container).setVisibility(View.GONE);
                    ((Button)view.findViewById(R.id.advanced_filter_btn)).setText(getString(R.string.advanced));
                }
                if(searchListener != null) searchListener.search(builder.build());
                break;
            }
            case R.id.time_max_filter:
            case R.id.time_min_filter:
            case R.id.start_date_filter:
            case R.id.end_date_filter:
                onFocusChange(v, true);
                break;
        }
    }
    private void showDateDialog(int editTextId){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    ((EditText)view.findViewById(editTextId)).setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", month + 1, day, year));
                }
            }, 2020, 1, 1);
            dialog.show();
        }
    }
    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    private int kkmmToInt(String formattedTime){
        int hours = Integer.parseInt(formattedTime.substring(0, formattedTime.indexOf(':')));
        int minutes = Integer.parseInt(formattedTime.substring(formattedTime.indexOf(':') + 1));
        return hours * 3600 + minutes;
    }
    private void showTimeDialog(int editTextId){
        TimePickerDialog dialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                ((EditText)view.findViewById(editTextId)).setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }
        }, 1, 0, true);
        dialog.show();
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if(b) {
            switch (view.getId()) {
                case R.id.start_date_filter:
                    showDateDialog(R.id.start_date_filter);
                    break;
                case R.id.end_date_filter:
                    showDateDialog(R.id.end_date_filter);
                    break;
                case R.id.time_min_filter:
                    showTimeDialog(R.id.time_min_filter);
                    break;
                case R.id.time_max_filter:
                    showTimeDialog(R.id.time_max_filter);
                    break;

            }
        }
    }
    public void clear(){

    }
}
class SearchFilters implements Serializable {
    public enum Bool{
        True, False, Ignore
    }
    public Date start = new Date(0), end = new Date(0);
    public int minTime = -1, maxTime = -1;
    public String gear = "", workoutType = "", raceType = "", name = "";
    public Bool trainer = Bool.Ignore;
    public float minSpeed = -1, maxSpeed = -1, minPow = -1, maxPow = -1, minElevation = -1, maxElevation = -1, minDist = -1, maxDist = -1;
    public boolean isDefaultValue(Object parameter){
        throw new IllegalArgumentException("Parameter not a type of a filter field!");
    }
    public boolean isDefaultValue(String parameter){
        return parameter == null || parameter.length() == 0;
    }
    public boolean isDefaultValue(Date parameter){
        return parameter.getTime() == 0;
    }
    public boolean isDefaultValue(int parameter){
        return parameter == -1;
    }
    public boolean isDefaultValue(float parameter){
        return parameter < 0;
    }
    public boolean isDefaultValue(Bool parameter){
        return parameter == Bool.Ignore;
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
