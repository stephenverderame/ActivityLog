package com.sev.activitylog;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class StravaModel extends RideModel {
    private OAuth auth;
    private ExecutorService executor;
    private LinkedList<Observer> observers;
    public StravaModel(OAuth authentication){
        auth = authentication;
        executor = Executors.newSingleThreadExecutor();
        observers = new LinkedList<>();
    }
    @Override
    public Future<LinkedList<RideOverview>> getRides(final long startDate) {
        return executor.submit(new Callable<LinkedList<RideOverview>>() {
            @Override
            public LinkedList<RideOverview> call() {
                if (auth.isAuthComplete()) {
                    Log.d("OVERVIEWS", "Auth Complete");
                    LinkedList<RideOverview> rideOverviews = new LinkedList<RideOverview>();
                    boolean success = false;
                    int pageNum = 1;
                    int retries = 0;
                    do {
                        try {
                            URL url = new URL("https://www.strava.com/api/v3/athlete/activities?after=" + (startDate / 1000) + "&per_page=100" + "&page=" + pageNum);
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
//                                    Log.d("OVERVIEWS", "Read " + r.getName());
                                        rideOverviews.push(r); //keeps everything in most recent order
                                    }
                                    if(ridesJson.length() == 0) success = false; //if no more rides
                                    else success = true;
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
                    ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, rideOverviews, System.currentTimeMillis()));
                    return rideOverviews;
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
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
}
