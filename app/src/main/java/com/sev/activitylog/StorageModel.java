package com.sev.activitylog;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StorageModel extends RideModel {
    /*
    rides.db FileFormat:
        long - epochTime of most recent activity
        int - num of lists
        List<LinkedList<RideOverview>> - list of activities
     */
    private Context context;
    private ExecutorService executor;
    private LinkedList<Observer> observers;
    private static final String DATA_FILE = "rides.db";
    private static final int CHUNK_SIZE = 100;
    public StorageModel(Context ctx){
        context = ctx;
        executor = Executors.newSingleThreadExecutor();
        observers = new LinkedList<Observer>();
    }
    @Override
    public Future<LinkedList<RideOverview>> getRides(long startDate) {
        return executor.submit(new Callable<LinkedList<RideOverview>>() {
            @Override
            public LinkedList<RideOverview> call() {
                LinkedList<RideOverview> rides = new LinkedList<RideOverview>();
                long lastSyncTime = 0;
                try(ObjectInputStream input = new ObjectInputStream(context.openFileInput(DATA_FILE))){
                    lastSyncTime = input.readLong();
                    int lists = input.readInt();
                    for(int i = 0; i < lists; ++i){
                        LinkedList<RideOverview> ride = (LinkedList<RideOverview>)input.readObject();
                        ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_PARTIAL_NOTIFY, ride, false));
                        rides.addAll(ride);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if(startDate != 0 && rides != null) {
                    for (int i = 0; i < rides.size(); ) {
                        if (rides.get(i).getDate().before(new Date(startDate))) {
                            rides.remove(i);
                        } else
                            ++i;
                    }
                }
                ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, rides, lastSyncTime));
                return rides;
            }
        });
    }

    /**
     * Serializes the linked list of rides. Overwrites old data so rides should include all of the old rides as well
     * @param rides ALL ride overviews
     * @param syncTime epoch time of last update
     */
    public void saveRides(LinkedList<RideOverview> rides, long syncTime){
        try(ObjectOutputStream output = new ObjectOutputStream(context.openFileOutput(DATA_FILE, Context.MODE_PRIVATE))){
            output.writeLong(syncTime);
            int lists = rides.size() / CHUNK_SIZE + 1;
            output.writeInt(lists);
            for(int i = 0; i < lists; ++i){
                LinkedList<RideOverview> list = new LinkedList<>();
                for(int j = i * CHUNK_SIZE; j < Math.min((i + 1) * CHUNK_SIZE, rides.size()); ++j)
                    list.add(rides.get(j));
                output.writeObject(list);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Future<DetailedRide> getRideDetails(long id) {
        return null;
    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
}
