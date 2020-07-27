package com.sev.activitylog;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

public class Util {
    private static final long BIG_NUM = System.currentTimeMillis();
    public static int rgba(float r, float g, float b, float a){
        return ((int)(a * 0xFF) << 24) | ((int)(r * 0xFF) << 16) | ((int)(g * 0xFF) << 8) | ((int)(b * 0xFF));
    }
    public static int rgba_v(int r, int g, int b, int a) {
        return ((int)(a) << 24) | ((int)(r) << 16) | ((int)(g) << 8) | ((int)(b));
    }
    public static int setAlpha(int color, float alpha){
        return ((int)(alpha * 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Computes interval seconds from the selected index of the interval list
     * @param num amount of intervals
     * @param item selected item in spinner
     * @return
     */
    public static long intervalSeconds(int num, FunctionalSpinnerItem item){
        long secs = 0; //interval activity
        num = Math.max(1, num);
        if(item.equals(FunctionalSpinnerItem.SPIN_INTERVAL_DAY)) secs = 24 * 3600 * num;
        else if(item.equals(FunctionalSpinnerItem.SPIN_INTERVAL_WEEK)) secs = 7 * 24 * 3600 * num;
        else if(item.equals(FunctionalSpinnerItem.SPIN_INTERVAL_MONTH)) secs = 4 * 7 * 24 * 3600 * num;
        else if(item.equals(FunctionalSpinnerItem.SPIN_INTERVAL_YEAR)) secs = 52 * 7 * 24 * 3600 * num;
        else if(item.equals(FunctionalSpinnerItem.SPIN_INTERVAL_NONE)) secs = BIG_NUM;
        return secs;
    }

    public static double getStat(RideStats r, FunctionalSpinnerItem item){
        if(item.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITY_DATE)) return r.getDate().getTime();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITIES)) return r.getCount();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITY_TYPE)) return toBase26(r.getActivityType());
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_AVG_SPEED)) return r.getAverageSpeed() * Settings.metersDistanceConversion() * 3600;
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_CALORIES)) return r.getKJ() / 4.184 * 4;
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_DISTANCE)) return r.getDistance() * Settings.metersDistanceConversion();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_ELEVATION)) return r.getClimbed() * Settings.metersElevationConversion();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_MV_TIME)) return r.getMovingTime();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_MX_SPEED)) return r.getMaxSpeed() * Settings.metersDistanceConversion() * 3600;
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_TL_TIME)) return r.getTotalTime();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_POWER)) return r.getPower();
        else if(item.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITY_INDEX)) return 0; //graphView will reformat data
        else throw new IllegalArgumentException(item.toString() + " is not a defined field!");
    }
    public static long toBase26(String str){
        long val = 0;
        str = str.toUpperCase();
        for(int i = 0; i < str.length(); ++i){
            int num = str.charAt(i) - 'A';
            if(num > 26 || num < 0) throw new IllegalArgumentException("Only alphabetic characters allowed!");
            val += num * Math.pow(26, str.length()  - 1 - i);
        }
        return val;
    }
    public static String fromBase26(long hash){
        LinkedList<Character> output = new LinkedList<>();
        do{
            output.push((char)((hash % 26) + 'A'));
            hash /= 26;
        } while(hash > 0);
        StringBuilder buffer = new StringBuilder();
        for(Character c : output)
            buffer.append(c);
        return buffer.toString();
    }
    public static String formatOutput(double val, FunctionalSpinnerItem stat){
        String output = null;
        if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_TL_TIME) || stat.equals(FunctionalSpinnerItem.SPIN_STAT_MV_TIME)){
            output = TimeSpan.fromSeconds((long)val);
        }else if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_ACTIVITY_TYPE)){
            output = fromBase26((long)val);
        }else if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_DISTANCE)){
            output = String.format(Locale.getDefault(), "%.2f %s", val * Settings.metersDistanceConversion(), Settings.distanceUnits());
        }else if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_ELEVATION)){
            output = String.format(Locale.getDefault(), "%.2f %s", val * Settings.metersElevationConversion(), Settings.elevationUnits());
        }
        else if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_AVG_SPEED) || stat.equals(FunctionalSpinnerItem.SPIN_STAT_MX_SPEED)){
            output = String.format(Locale.getDefault(), "%.2f %s", val * Settings.metersDistanceConversion() * 3600, Settings.speedUnits());
        }
        else if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_POWER)){
            output = String.format(Locale.getDefault(), "%.2f W", val);
        }
        else if(stat.equals(FunctionalSpinnerItem.SPIN_STAT_CALORIES))
            output = String.format(Locale.getDefault(), "%.2f kCal", val);
        else
            output = String.format(Locale.getDefault(), "%.2f", val);
        return output;
    }
    public static double computeFunction(double[] values, FunctionalSpinnerItem function){
        double output = 0;
        if(function.equals(FunctionalSpinnerItem.SPIN_FUNC_MEAN)){
            for(double v : values)
                output += v;
            output /= values.length;
        }else if(function.equals(FunctionalSpinnerItem.SPIN_FUNC_SUM)){
            for(double v : values)
                output += v;
        }else if(function.equals(FunctionalSpinnerItem.SPIN_FUNC_MAX)){
            output = Integer.MIN_VALUE;
            for(double v : values)
                if(v > output) output = v;
        }else if(function.equals(FunctionalSpinnerItem.SPIN_FUNC_MIN)){
            output = Integer.MAX_VALUE;
            for(double v : values)
                if(v < output) output = v;
        }
        else if(function.equals(FunctionalSpinnerItem.SPIN_FUNC_MODE)){
            HashMap<Integer, Integer> map = new HashMap<>();
            for(double v : values) {
                if (map.containsKey(v)) map.put((int)Math.round(v), map.get((int)Math.round(v)) + 1);
                else map.put((int)Math.round(v), 1);
            }
            int max = Integer.MIN_VALUE;
            for(double v : values){
                if(map.get((int)Math.round(v)) > max){
                    output = v;
                    max = map.get((int)Math.round(v));
                }
            }
        }else if(function.equals(FunctionalSpinnerItem.SPIN_FUNC_MEDIAN)){
            //O(n) median implemented similarly to quicksort. See algorithm notes for explanation
            int pivot = partition(values, 0, values.length - 1);
            int low = 0, high = values.length - 1;
            while(pivot != values.length / 2){
                if(pivot > values.length / 2)
                    pivot = partition(values, low, pivot - 1);
                else
                    pivot = partition(values, pivot + 1, high);
            }
            output = values[pivot];
        }else throw new IllegalArgumentException(function.toString() + " is undefined!");
        return output;
    }

    /**
     * Partitions the list into two sections: values below the pivot and values above the pivot
     * Helper function for fast median
     * @param list
     * @param low index of starting location
     * @param high index of ending location
     * @return the index of where the range between low and high is separated (the index of the pivot)
     */
    private static int partition(double[] list, int low, int high){
        int pivot = high;
        int firstHigh = low;
        for(int i = low; i < high; ++i){
            if(list[i] < list[pivot]){
                double temp = list[firstHigh];
                list[firstHigh++] = list[i];
                list[i] = temp;
            }
        }
        double temp = list[firstHigh];
        list[firstHigh] = list[pivot];
        list[pivot] = temp;
        return firstHigh;
    }

}
