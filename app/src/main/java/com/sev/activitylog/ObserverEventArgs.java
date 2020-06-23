package com.sev.activitylog;

public class ObserverEventArgs {
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
