package com.sev.activitylog;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

import javax.net.ssl.HttpsURLConnection;

public class StravaModel extends RemoteModel{
    private OAuth auth;
    private ExecutorService executor;
    public StravaModel(OAuth authentication){
        auth = authentication;
        executor = Executors.newSingleThreadExecutor();
    }
    @Override
    public Future<LinkedList<RideOverview>> getRides() {
        return executor.submit(new Callable<LinkedList<RideOverview>>() {
            @Override
            public LinkedList<RideOverview> call() throws Exception {
                if (auth.isAuthComplete()) {
                    Log.d("OVERVIEWS", "Auth Complete");
                    LinkedList<RideOverview> rideOverviews = new LinkedList<RideOverview>();
                    try {
                        URL url = new URL("https://www.strava.com/api/v3/athlete/activities?page=1&per_page=30");
                        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                        con.setDoInput(true);
                        con.setDoOutput(false);
                        con.setRequestMethod("GET");
                        con.setRequestProperty("Authorization", "Bearer " + auth.getAccessToken());
                        con.setRequestProperty("Accept", "application/json, */*");
                        con.setRequestProperty("Host", "www.strava.com");
                        con.setRequestProperty("Connection", "keep-alive");
                        con.connect();
                        Log.d("OVERVIEWS", con.getResponseMessage());
                        if(con.getResponseCode() == 200) {
                            Log.d("OVERVIEWS", "Got response");
                            try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                                StringBuilder content = new StringBuilder();
                                String line;
                                while((line = reader.readLine()) != null){
                                    content.append(line);
                                }
                                JSONArray ridesJson = new JSONArray(content.toString());
                                for (int i = 0; i < ridesJson.length(); ++i) {
                                    JSONObject ride = ridesJson.getJSONObject(i);
                                    RideOverview r = new RideOverview(ride.getString("name"), ride.getString("id"));
                                    r.setDistance(ride.getDouble("distance"));
                                    r.setMovingTime(ride.getInt("moving_time"));
                                    r.setTotalTime(ride.getInt("elapsed_time"));
                                    r.setDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(ride.getString("start_date_local")));
                                    r.setClimbed(ride.getDouble("total_elevation_gain"));
//                                    Log.d("OVERVIEWS", "Read " + r.getName());
                                    rideOverviews.add(r);
                                }
                            }
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.e("STRAVA", "Date parse exception");
                    }

                    return rideOverviews;
                }
                return null;
            }
        });
    }

    @Override
    public Future<DetailedRide> getRideDetails(RideOverview overview) {
        return executor.submit(new Callable<DetailedRide>() {
            @Override
            public DetailedRide call() throws Exception {
                return null;
            }
        });
    }


}
