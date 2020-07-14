package com.sev.activitylog;

public class TimeSpan {
    /**
     * Converts an integer amount of seconds to a String in HH:mm:ss format
     * @param seconds
     * @return string in HH:mm:ss format
     */
    public static String fromSeconds(long seconds){
        StringBuilder time = new StringBuilder();
        float leftoverTime = seconds;
        if(seconds >= 3600){
            time.append(seconds / 3600);
            time.append(':');
            leftoverTime = seconds / 3600.f;
            leftoverTime -= (int)leftoverTime;
            leftoverTime *= 3600;
        }
        time.append(String.format("%02d", (int)leftoverTime / 60));
        time.append(':');
        leftoverTime /= 60.f;
        leftoverTime -= (int)leftoverTime;
        leftoverTime *= 60;
        time.append(String.format("%02d", (int)Math.round(leftoverTime)));
        return time.toString();
    }
}
