package com.sev.activitylog;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FunctionalSpinnerItem {
    private final SpinnerItemType type;
    private final int ID;
    private FunctionalSpinnerItem(SpinnerItemType type, int ID){
        this.type = type;
        this.ID = ID;
    }

    public String getText(Resources res){
        return res.getString(ID);
    }

    public boolean equals(FunctionalSpinnerItem item) {
        return item.type == type && item.ID == ID;
    }
    public int getID() {
        return ID;
    }

    public static final FunctionalSpinnerItem SPIN_INTERVAL_ACTIVITY = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_INTERVAL, R.string.int_activity);
    public static final FunctionalSpinnerItem SPIN_INTERVAL_DAY = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_INTERVAL, R.string.int_day);
    public static final FunctionalSpinnerItem SPIN_INTERVAL_WEEK = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_INTERVAL, R.string.int_week);
    public static final FunctionalSpinnerItem SPIN_INTERVAL_MONTH = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_INTERVAL, R.string.int_month);
    public static final FunctionalSpinnerItem SPIN_INTERVAL_YEAR = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_INTERVAL, R.string.int_year);
    public static final FunctionalSpinnerItem SPIN_INTERVAL_NONE = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_INTERVAL, R.string.int_none);

    public static final FunctionalSpinnerItem SPIN_STAT_ACTIVITIES = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_activities);
    public static final FunctionalSpinnerItem SPIN_STAT_ACTIVITY_TYPE = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_activity_type);
    public static final FunctionalSpinnerItem SPIN_STAT_AVG_SPEED = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_speed);
    public static final FunctionalSpinnerItem SPIN_STAT_CALORIES = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_calories);
    public static final FunctionalSpinnerItem SPIN_STAT_DISTANCE = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_dist);
    public static final FunctionalSpinnerItem SPIN_STAT_ELEVATION = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_climb);
    public static final FunctionalSpinnerItem SPIN_STAT_MX_SPEED = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_mx_speed);
    public static final FunctionalSpinnerItem SPIN_STAT_MV_TIME = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_time);
    public static final FunctionalSpinnerItem SPIN_STAT_TL_TIME = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_ttime);
    public static final FunctionalSpinnerItem SPIN_STAT_POWER = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_STAT, R.string.stat_pow);

    public static final FunctionalSpinnerItem SPIN_FUNC_MAX = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_FUNCTION, R.string.func_max);
    public static final FunctionalSpinnerItem SPIN_FUNC_MEAN = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_FUNCTION, R.string.func_mean);
    public static final FunctionalSpinnerItem SPIN_FUNC_MEDIAN = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_FUNCTION, R.string.func_med);
    public static final FunctionalSpinnerItem SPIN_FUNC_MIN = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_FUNCTION, R.string.func_min);
    public static final FunctionalSpinnerItem SPIN_FUNC_MODE = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_FUNCTION, R.string.func_mode);
    public static final FunctionalSpinnerItem SPIN_FUNC_SUM = new FunctionalSpinnerItem(SpinnerItemType.SPINNER_TYPE_FUNCTION, R.string.func_sum);

    public static final FunctionalSpinnerItem[] SPIN_FUNC_LIST = new FunctionalSpinnerItem[] {SPIN_FUNC_MAX, SPIN_FUNC_MEAN, SPIN_FUNC_MEDIAN, SPIN_FUNC_MIN, SPIN_FUNC_MODE, SPIN_FUNC_SUM};
    public static final FunctionalSpinnerItem[] SPIN_STAT_LIST = new FunctionalSpinnerItem[] {SPIN_STAT_ACTIVITIES, SPIN_STAT_ACTIVITY_TYPE, SPIN_STAT_AVG_SPEED, SPIN_STAT_CALORIES, SPIN_STAT_DISTANCE,
                                                                                                SPIN_STAT_ELEVATION, SPIN_STAT_MV_TIME, SPIN_STAT_MX_SPEED, SPIN_STAT_POWER, SPIN_STAT_TL_TIME};
    public static final FunctionalSpinnerItem[] SPIN_INT_LIST = new FunctionalSpinnerItem[] {SPIN_INTERVAL_ACTIVITY, SPIN_INTERVAL_DAY, SPIN_INTERVAL_WEEK, SPIN_INTERVAL_MONTH, SPIN_INTERVAL_YEAR, SPIN_INTERVAL_NONE};
}
enum SpinnerItemType {
    SPINNER_TYPE_STAT,
    SPINNER_TYPE_INTERVAL,
    SPINNER_TYPE_FUNCTION,
}
class FunctionalSpinAdapter extends ArrayAdapter<FunctionalSpinnerItem> {

    private FunctionalSpinnerItem[] items;
    private Context ctx;
    public FunctionalSpinAdapter(@NonNull Context context, int resource, @NonNull FunctionalSpinnerItem[] objects) {
        super(context, resource, objects);
        this.ctx = context;
        items = objects;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        TextView v = (TextView)super.getView(position, convertView, parent);
        v.setText(ctx.getResources().getString(items[position].getID()));
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
        TextView v = (TextView)super.getDropDownView(position, convertView, parent);
        v.setText(ctx.getResources().getString(items[position].getID()));
        return v;
    }
}
