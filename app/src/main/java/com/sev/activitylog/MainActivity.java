package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.view.View;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements Observer {

    private LinkedList<RideOverview> rideList;
    private StorageModel storage;
    long lastSync = 0;
    private OAuth auth;
    private RecyclerView recyclerView;
    private CardListAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        rideList = new LinkedList<RideOverview>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storage = new StorageModel(this);
        storage.attach(this);
        storage.getRides(0);
 //       notify(new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, null, null)); //DEBUGGING - to force update from strava

        recyclerView = (RecyclerView)findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new CardListAdapter(rideList);
        recyclerViewAdapter.attach(this);
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public void notify(ObserverEventArgs e) {
        switch (e.getEventType()) {
            case OAUTH_NOTIFY: {
                auth = (OAuth) e.getEventArgs()[0];
                if (auth.isAuthComplete()) {
                    StravaModel model = new StravaModel(auth);
                    model.attach(this);
                    model.getRides(lastSync);
                }
                break;

            }
            case URI_NOTIFY:
                startActivity((Intent) e.getEventArgs()[0]);
                break;
            case RIDES_LOAD_NOTIFY: {
                if(e.getEventArgs()[1] != null)
                    lastSync = (long) e.getEventArgs()[1];
                if(e.getEventArgs()[0] != null) {
                    LinkedList<RideOverview> rides = (LinkedList<RideOverview>) e.getEventArgs()[0];
                    rideList.addAll(rides);
                }
                if(System.currentTimeMillis() - lastSync > 1000 * 3600 * 24){ //data outdated, get new data from remote server
                    OAuth auth = new OAuth(new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE)));
                    auth.attach(this);
                    Thread t = new Thread(auth);
                    t.start();
                }else {
//                    populate(rideList);
                    storage.saveRides(rideList, lastSync);
                }
                break;
            }
            case RIDES_LOAD_PARTIAL_NOTIFY:
            {
                if(findViewById(R.id.loadingBar) != null){
                    LinearLayout layout = (LinearLayout) findViewById(R.id.container);
                    layout.removeView(findViewById(R.id.loadingBar));
                    layout.removeView(findViewById(R.id.loadingText));
                }
                if(e.getEventArgs()[0] != null){
                    LinkedList<RideOverview> newRides = (LinkedList<RideOverview>)e.getEventArgs()[0];
                    boolean insert = (boolean)e.getEventArgs()[1];
                    if(insert) {
                        rideList.addAll(0, newRides);
                        recyclerViewAdapter.notifyItemRangeInserted(0, newRides.size());
                        recyclerView.scrollToPosition(0);
                    }
                    else{
                        rideList.addAll(newRides);
                        recyclerViewAdapter.notifyItemRangeChanged(rideList.size() - newRides.size(), newRides.size());
                    }
                }
                break;
            }
            case ACTIVITY_SELECT_NOTIFY:
            {
                Intent detailedIntent = new Intent(this, DetailedActivityView.class);
                detailedIntent.putExtra("activity_id", (long)e.getEventArgs()[0]);
                if(auth != null)
                    detailedIntent.putExtra("auth_token", auth.getAuthToken());
                startActivity(detailedIntent);
                break;
            }
        }
    }

    public void populate(LinkedList<RideOverview> rides) {
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        layout.removeView(findViewById(R.id.loadingBar));
        layout.removeView(findViewById(R.id.loadingText));
 /*       for (RideOverview ride : rides) {
            ActivityView view = new ActivityView(this);
            view.setTitle(ride.getName());
            Date d = ride.getDate();
            view.setSubtitle(new SimpleDateFormat("MMM dd yyyy hh:mm a").format(ride.getDate()));
            view.setInfoGrid(1, 4);
            view.setInfo("Distance", String.format("%.2f miles", ride.getDistance() * 0.000621371), 0, 0);
            view.setInfo("Time", TimeSpan.fromSeconds((long) ride.getMovingTime()), 0, 1);
            view.setInfo("Elevation", String.format("%.2f ft", ride.getClimbed() * 3.28084), 0, 2);
            view.setInfo("Type", ride.getActivityType(), 0, 3);
            view.setId(ride.getId());
            view.attach(this);
            view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().density * 100)));
            float dp = getResources().getDisplayMetrics().density;
            view.setPadding(new PaddingBuilder().top(20).left(20).right(20).build());
            view.setFont(new ActivityViewFontBuilder().titleSize(18).subtitleSize(8).labelSize(10).infoSize(6).build());
            view.setInfoPadding(5);
            layout.addView(view);
        }*/
        CardListAdapter adapter = new CardListAdapter(rides);
        adapter.attach(this);
        recyclerView.setAdapter(adapter);
    }
}