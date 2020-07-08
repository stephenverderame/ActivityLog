package com.sev.activitylog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * Gets a list of all the user's gear. Output may either be null, a complete list, or a list containing IDs only.
     * @param incompleteFlag output parameter. If true, flags the output as a list containing IDs only.
     * @return
     */
    public abstract Future<ArrayList<Gear>> getGear(AtomicBoolean incompleteFlag);
}
