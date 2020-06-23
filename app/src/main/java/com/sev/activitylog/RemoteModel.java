package com.sev.activitylog;

import java.util.LinkedList;
import java.util.concurrent.Future;

public abstract class RemoteModel {

    public abstract Future<LinkedList<RideOverview>> getRides();
    public abstract Future<DetailedRide> getRideDetails(RideOverview overview);
}
