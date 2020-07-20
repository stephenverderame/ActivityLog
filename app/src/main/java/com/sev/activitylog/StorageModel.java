package com.sev.activitylog;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final String CACHE_FILE = "cache.db";
    private static final String GEAR_FILE = "gear.db";
    private static final int CHUNK_SIZE = 100;
    private UniqueList<String> gearIds;
    public StorageModel(Context ctx){
        context = ctx;
        executor = Executors.newSingleThreadExecutor();
        observers = new LinkedList<Observer>();
        gearIds = new UniqueList<>();
    }
    @Override
    public Future<LinkedList<RideOverview>> getRides(long startDate) {
        return executor.submit(new Callable<LinkedList<RideOverview>>() {
            @Override
            public LinkedList<RideOverview> call() {
                LinkedList<RideOverview> rides = new LinkedList<RideOverview>();
                long lastSyncTime = 0;
                synchronized(DATA_FILE){
                    try (ObjectInputStream input = new ObjectInputStream(context.openFileInput(DATA_FILE));) {
                        lastSyncTime = input.readLong();
                        int lists = input.readInt();
                        for (int i = 0; i < lists; ++i) {
                            LinkedList<RideOverview> ride = (LinkedList<RideOverview>) input.readObject();
                            ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_PARTIAL_NOTIFY, ride, false));
                            rides.addAll(ride);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e){
                        lastSyncTime = 0;
                        e.printStackTrace();
                    }
                }
                if(startDate != 0 && rides != null) {
                    for (int i = 0; i < rides.size(); ) {
                        if (rides.get(i).getDate().before(new Date(startDate))) {
                            rides.remove(i);
                        } else
                            ++i;
                    }
                }
                for(RideOverview r : rides){
                    gearIds.add(r.getGearId());
                }
                ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, lastSyncTime));
                return rides;
            }
        });
    }

    /**
     * Serializes the linked list of rides. Overwrites old data so rides should include all of the old rides as well
     * @param rides ALL ride overviews
     * @param syncTime epoch time of last update
     */
    public void saveRides(List<RideOverview> rides, long syncTime){
        synchronized (DATA_FILE){
            try (ObjectOutputStream output = new ObjectOutputStream(context.openFileOutput(DATA_FILE, Context.MODE_PRIVATE))) {
                output.writeLong(syncTime);
                int lists = rides.size() / CHUNK_SIZE + 1;
                output.writeInt(lists);
                for (int i = 0; i < lists; ++i) {
                    LinkedList<RideOverview> list = new LinkedList<>();
                    for (int j = i * CHUNK_SIZE; j < Math.min((i + 1) * CHUNK_SIZE, rides.size()); ++j)
                        list.add(rides.get(j));
                    output.writeObject(list);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Future<DetailedRide> getRideDetails(long id) {
        return executor.submit(new Callable<DetailedRide>() {
            @Override
            public DetailedRide call() {
                synchronized (CACHE_FILE){
                    try(ObjectInputStream input = new ObjectInputStream(context.openFileInput(CACHE_FILE))){
                        CircularLinkedList<Long> ids = (CircularLinkedList<Long>)input.readObject();
                        boolean cont = false;
                        int i = 0;
                        for(; i < ids.size(); ++i){
                            if(ids.get(i) == id){
                                cont = true;
                                break;
                            }
                        }
                        if(cont){
                            CircularLinkedList<DetailedRide> data = (CircularLinkedList<DetailedRide>)input.readObject();
                            return data.get(i);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        });
    }

    @Override
    public Future<ArrayList<Gear>> getGear(AtomicBoolean incompleteFlag) {
        return executor.submit(new Callable<ArrayList<Gear>>() {
            @Override
            public ArrayList<Gear> call() {
                synchronized (GEAR_FILE) {
                    try(ObjectInputStream input = new ObjectInputStream(context.openFileInput(GEAR_FILE))){
                        ArrayList<Gear> list = (ArrayList<Gear>)input.readObject();
                        if(list.size() >= 1 && list.get(0).gearName != null) incompleteFlag.set(false);
                        else if(list.size() == 0) throw new IllegalStateException("No gears saved");
                        return list;
                    } catch (Exception e) {
                        if(gearIds.size() >= 1){
                            incompleteFlag.set(true);
                            ArrayList<Gear> gears = new ArrayList<>(gearIds.size());
                            for(String id : gearIds)
                                gears.add(new Gear(id));
                            return gears;
                        }
                    }
                }
                return null;
            }
        });
    }
    public void saveGearList(ArrayList<Gear> gears){
        synchronized (GEAR_FILE){
            try(ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput(GEAR_FILE, Context.MODE_PRIVATE))){
                out.writeObject(gears);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveDetailedRide(DetailedRide ride){
        synchronized (CACHE_FILE){
            CircularLinkedList<Long> ids;
            CircularLinkedList<DetailedRide> rides;
            try(ObjectInputStream input = new ObjectInputStream(context.openFileInput(CACHE_FILE))){
                ids = (CircularLinkedList<Long>)input.readObject();
                rides = (CircularLinkedList<DetailedRide>)input.readObject();
            } catch (Exception e){
                e.printStackTrace();
                ids = new CircularLinkedList<>(Long.class, 100);
                rides = new CircularLinkedList<>(DetailedRide.class, 100);
            }
            ids.add(ride.getOverview().getId());
            rides.add(ride);
            try(ObjectOutputStream out = new ObjectOutputStream(context.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE))){
                out.writeObject(ids);
                out.writeObject(rides);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void deleteCache(Context ctx){
        synchronized (CACHE_FILE) { ctx.deleteFile(CACHE_FILE); }
        synchronized (GEAR_FILE) { ctx.deleteFile(GEAR_FILE); }
        synchronized (DATA_FILE) { ctx.deleteFile(DATA_FILE); }
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
