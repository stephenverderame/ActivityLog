package com.sev.activitylog;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

public class StravaModel extends RideModel {
    private OAuth auth;
    private ExecutorService executor;
    private LinkedList<Observer> observers;
    private UniqueList<String> gearIds;
    public StravaModel(OAuth authentication){
        auth = authentication;
        executor = Executors.newSingleThreadExecutor();
        observers = new LinkedList<>();
        gearIds = new UniqueList<>();
    }
    @Override
    public Future<LinkedList<RideOverview>> getRides(final long startDate) {
        return executor.submit(new Callable<LinkedList<RideOverview>>() {
            @Override
            public LinkedList<RideOverview> call() {
                if (auth.isAuthComplete()) {
                    Log.d("OVERVIEWS", "Auth Complete");
                    boolean success = false;
 //                   LinkedList<RideOverview> rideOverviews = new LinkedList<RideOverview>();
                    int pageNum = 1;
                    int retries = 0;
                    do {
                        try {
                            LinkedList<RideOverview> buffer = new LinkedList<RideOverview>();
                            String after = startDate == 0 ? "" : ("after=" + (startDate / 1000)); //strava sends data most recent first when not using the after tag, otherwise it goes ascending order from that date
                            URL url = new URL("https://www.strava.com/api/v3/athlete/activities?" + after + "&per_page=30" + "&page=" + pageNum);
                            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                            con.setDoInput(true);
                            con.setDoOutput(false);
                            con.setRequestMethod("GET");
                            con.setRequestProperty("Authorization", "Bearer " + auth.getAuthToken().getAccessToken());
                            con.setRequestProperty("Accept", "application/json, */*");
                            con.setRequestProperty("Host", "www.strava.com");
                            con.setRequestProperty("Connection", "keep-alive");
                            con.connect();
                            Log.d("OVERVIEWS", con.getResponseMessage());
                            if (con.getResponseCode() == 200) {
                                Log.d("OVERVIEWS", "Got response");
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                                    StringBuilder content = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        content.append(line);
                                    }
                                    JSONArray ridesJson = new JSONArray(content.toString());
                                    for (int i = 0; i < ridesJson.length(); ++i) {
                                        JSONObject ride = ridesJson.getJSONObject(i);
                                        RideOverview r = new RideOverview(ride);
                                        gearIds.add(r.getGearId());
//                                    Log.d("OVERVIEWS", "Read " + r.getName());
                                        if(startDate != 0 ) buffer.push(r);
                                        else buffer.add(r);
 //                                       rideOverviews.add(r);
                                    }
                                    if(ridesJson.length() == 0) success = false; //if no more rides
                                    else {
                                        success = true;
                                        ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_PARTIAL_NOTIFY, buffer, startDate != 0));
                                    }
                                }
                            }else success = false;
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            success = false;
                        } catch (IOException e) {
                            e.printStackTrace();
                            success = false;
                        } catch (JSONException e) {
                            e.printStackTrace();
                            success = false;
                        } catch (ParseException e) {
                            e.printStackTrace();
                            success = false;
                            Log.e("STRAVA", "Date parse exception");
                        }
                        ++pageNum;

                    }while(success);
                    ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, System.currentTimeMillis()));
                    return null;
                }
                return null;
            }
        });
    }

    @Override
    public Future<DetailedRide> getRideDetails(long id) {
        return executor.submit(new Callable<DetailedRide>() {
            @Override
            public DetailedRide call() {
                DetailedRide ride = null;
                Log.d("DETAILS", "Call");
                try {
                    URL url = new URL("https://www.strava.com/api/v3/activities/" + id);
                    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                    con.setDoInput(true);
                    con.setDoOutput(false);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Host", "www.strava.com");
                    con.setRequestProperty("Accept", "application/json, */*");
                    con.setRequestProperty("Authorization", "Bearer " + auth.getAuthToken().getAccessToken());
                    con.connect();
                    if(con.getResponseCode() == 200){
                        Log.d("DETAILS", "Got detailed ride!");
                        try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                            String line;
                            StringBuilder json = new StringBuilder();
                            while((line = reader.readLine()) != null)
                                json.append(line);
                            ride = new DetailedRide(new JSONObject(json.toString()));
                        }
                    }else
                        Log.e("DETAILS", con.getResponseCode() + con.getResponseMessage());
                } catch (MalformedURLException e) {
                    Log.e("DETAILS", e.toString());
                } catch (ProtocolException e) {
                    Log.e("DETAILS", e.toString());
                } catch (IOException e) {
                    Log.e("DETAILS", e.toString());
                } catch (JSONException e) {
                    Log.e("DETAILS", e.toString());
                } catch (ParseException e) {
                    Log.e("DETAILS", e.toString());
                }
                ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDE_DETAILS_NOTIFY, ride));
                return ride;
            }
        });
    }

    @Override
    public Future<ArrayList<Gear>> getGear(AtomicBoolean incompleteFlag) {
        return executor.submit(new Callable<ArrayList<Gear>>() {
            @Override
            public ArrayList<Gear> call() {
                ArrayList<Gear> gears = new ArrayList<>();
                for(String id : gearIds){
                    try {
                        URL url = new URL("https://www.strava.com/api/v3/gear/" + id);
                        HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                        con.setDoOutput(false);
                        con.setDoInput(true);
                        con.setRequestMethod("GET");
                        con.setRequestProperty("Accept", "application/json");
                        con.setRequestProperty("Authorization", "Bearer " + auth.getAuthToken().getAccessToken());
                        con.setRequestProperty("Host", "www.strava.com");
                        con.connect();
                        if(con.getResponseCode() == 200){
                            Log.d("Strava", "Got Gear");
                            try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                                String line, json = new String();
                                while((line = reader.readLine()) != null)
                                    json += line;
                                gears.add(new Gear(new JSONObject(json)));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else
                            Log.e("Strava", "Failed to get gear with id " + id);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                incompleteFlag.set(false);
                return gears;
            }
        });
    }
    public void addGearIds(ArrayList<Gear> ids){
        for(Gear i : ids)
            gearIds.add(i.gearId);
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
