package com.sev.activitylog;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MonthFragment extends Fragment implements View.OnTouchListener {
    private Date month;
    private int totalDays, startingDay;
    private String monthName;
    private Observer controller;
    private List<RideOverview> rides;
    private float touchX = -1, touchY = -1;
    private View[] dayViews;
    private View view;
    public MonthFragment(Date month, List<RideOverview> rides, Observer mediator){
        this.month = month;
        controller = mediator;
        this.rides = rides;
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(view == null) {
            view = inflater.inflate(R.layout.month_view, null);
            GridLayout grid = (GridLayout) view.findViewById(R.id.monthGrid);
            dayViews = new View[6 * 7];
            for (int i = 0; i < 6; ++i) {
                for (int d = 0; d < 7; ++d) {
                    View v = inflater.inflate(R.layout.month_item, null);
                    v.findViewById(R.id.monthItemNum).setVisibility(View.INVISIBLE);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.columnSpec = GridLayout.spec(d, 1, 1); //index, span, weight
                    params.rowSpec = GridLayout.spec(i + 3, 1);
                    params.setGravity(Gravity.FILL);
//                    params.width = 0;
 //                   params.height = 0;
                    v.setLayoutParams(params);
                    grid.addView(v);
                    dayViews[i * 7 + d] = v;
                }
            }
            view.findViewById(R.id.monthViewName).setOnTouchListener(this);
            recreate();
        }
        return view;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
/*        final int changeThreshold = 10;
        switch(motionEvent.getActionMasked()){
            case MotionEvent.ACTION_MOVE: {
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                float dx = x - touchX;
                Calendar c = Calendar.getInstance();
                c.setTime(month);
                if(dx > changeThreshold) {
                    month = new Date(month.getTime() - (c.get(Calendar.DAY_OF_MONTH) + 1) * 3600 * 24 * 1000L);
                    recreate();
                }else if(dx < -changeThreshold){
                    month = new Date(month.getTime() + ((c.getActualMaximum(Calendar.DAY_OF_MONTH ) - c.get(Calendar.DAY_OF_MONTH)) + 1) * 3600 * 24 * 1000L);
                    recreate();
                }
                touchX = x;
                touchY = y;
                break;
            }
            case MotionEvent.ACTION_DOWN:
                touchX = motionEvent.getX();
                touchY = motionEvent.getY();
                break;

        }*/
        if(motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
            MonthYearPickerDialog dialog = new MonthYearPickerDialog();
            dialog.setListener(new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, i);
                    cal.set(Calendar.MONTH, i1);
                    month = cal.getTime();
                    recreate();
                }
            });
            dialog.show(getFragmentManager(), "MonthYearPickerDialog");
        }
        return true;
    }

    private void recreate() {
        if(month == null) month = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        startingDay = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; //index 0 (mon) to 6 (sun)
        monthName = new SimpleDateFormat("MMMM yyyy").format(month);
        ((TextView)view.findViewById(R.id.monthViewName)).setText(monthName);
        int days = 0;
        for(int i = 0; i < 6; ++i){
            for(int d = 0; d < 7; ++d){
                View v = dayViews[i * 7 + d];
                if(((i == 0 && d >= startingDay) || i > 0) && days < totalDays) {
                    TextView number = (TextView) v.findViewById(R.id.monthItemNum);
                    number.setText(String.valueOf(days + 1));
                    number.setVisibility(View.VISIBLE);
                    cal.set(Calendar.DAY_OF_MONTH, days + 1);
                    if(rides != null) {
                        int ride = RideOverview.indexOfExactDay(rides, cal.getTime());
                        if (ride != -1) {
                            CardView indicator = ((CardView) v.findViewById(R.id.monthRideIndicator));
                            indicator.setVisibility(View.VISIBLE);
                            int exertion = rides.get(ride).getExertion();
                            indicator.setCardBackgroundColor(exertion <= 5 ? Util.rgba(exertion / 5.f, 1.f, 0, 1) : Util.rgba(1.0f, (10 - exertion) / 4.f, 0, 1));
                            if (controller != null)
                                v.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        controller.notify(new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_NOTIFY, rides.get(ride)));
                                    }
                                });

                        }else{
                            v.findViewById(R.id.monthRideIndicator).setVisibility(View.INVISIBLE);
                            v.setOnClickListener(null);
                        }
                    }
                    ++days;
                }else {
                    v.findViewById(R.id.monthItemNum).setVisibility(View.INVISIBLE);
                    v.findViewById(R.id.monthRideIndicator).setVisibility(View.INVISIBLE);
                    v.setOnClickListener(null);
                }
            }
        }
        view.invalidate();
    }
}
