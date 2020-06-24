package com.sev.activitylog;

import java.util.LinkedList;
import java.util.concurrent.Future;

public abstract class RideModel implements Subject {

    /**
     * Gets all rides asynchronously that occur after the startDate
     * Should call ObserverHelper.notifyObservers on completion to notify observers that the data has been loaded
     * Args for ObserverHelper can be null
     * @param startDate milliseconds since epoch
     * @return a future to the list of ride overviews
     * @see ObserverHelper
     */
    public abstract Future<LinkedList<RideOverview>> getRides(final long startDate);
    public abstract Future<DetailedRide> getRideDetails(long id);
}
