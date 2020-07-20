package com.sev.activitylog;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;

public interface Observer {
    public void notify(ObserverEventArgs e);

}

class ObserverEventArgs {
    private ObserverNotifications eventType;
    private Object[] args;
    ObserverEventArgs(ObserverNotifications type, Object ...args){
        eventType = type;
        this.args = new Object[args.length];
        for(int i = 0; i < args.length; ++i)
            this.args[i] = args[i];
    }
    Object[] getEventArgs() {return args;}
    ObserverNotifications getEventType() {return eventType;}
}

enum ObserverNotifications {
    URI_NOTIFY, //args {Intent}
    OAUTH_NOTIFY, //args {OAuth, Boolean}
    RIDES_LOAD_NOTIFY, //args {long lastActivityTime}
    RIDE_DETAILS_NOTIFY, //args {DetailedRide}
    MAP_LOAD_NOTIFY, //args {MapImage}
    ACTIVITY_SELECT_NOTIFY, //args {RideOverview ride, View clickedView}
    RIDES_LOAD_PARTIAL_NOTIFY, //args {LinkedList<RideOverview>, bool insertOrAdd (true for insert)}
    ACTIVITY_SELECT_MULTIPLE_NOTIFY, //args {ArrayList<RideOverview> ids, View}
    OPENGL_INIT_NOTIFY,
    TOUCH_NOTIFY, //args MotionEvent
    REDRAW_NOTIFY
}
interface Subject {
    public void attach(Observer observer);
    public void detach(Observer observer);
}
class ObserverHelper{
    /**
     * Sends a notification to all observers of a subject. Can be called from any thread or async task as the notifications will be handled on the app's main thread
     * @param obs list of observers
     * @param e notification event
     */
    public static void sendToObservers(final LinkedList<Observer> obs, final ObserverEventArgs e){
        new Handler(Looper.getMainLooper()).post(() -> { //Posts a runnable to be run on the main thread. Android only
            for(Observer o : obs){
                o.notify(e);
            }
        });
    }
    public static void sendTo(final Observer ob, final ObserverEventArgs e){
        new Handler(Looper.getMainLooper()).post(() -> {
                ob.notify(e);
        });
    }
}

