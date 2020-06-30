package com.sev.activitylog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements Observer {

    private LinkedList<RideOverview> rideList;
    private StorageModel storage;
    long lastSync = 0;
    private OAuth auth;
    private RecyclerView recyclerView;
    private RecentActivityAdapter recyclerViewAdapter; //adapter for per activity view
    private WeekViewAdapter recyclerWeekAdapter; //adapter for per week view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        rideList = new LinkedList<RideOverview>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storage = new StorageModel(this);
        storage.attach(this);
        storage.getRides(0);
//        notify(new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, null, null)); //DEBUGGING - to force update from strava

        recyclerView = (RecyclerView)findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecentActivityAdapter(rideList);
        recyclerViewAdapter.attach(this);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerWeekAdapter = new WeekViewAdapter(rideList);
        recyclerWeekAdapter.attach(this);

        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.navBar);
        nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.recentMenuItem:
                        recyclerView.setAdapter(recyclerViewAdapter);
                        break;
                    case R.id.weekMenuItem:
                        recyclerWeekAdapter.init();
                        recyclerView.setAdapter(recyclerWeekAdapter);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
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
                if(e.getEventArgs()[0] != null)
                    lastSync = (long) e.getEventArgs()[0];
                if(System.currentTimeMillis() - lastSync > 1000 * 3600 * 24){ //data outdated, get new data from remote server
                    OAuth auth = new OAuth(new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE)));
                    auth.attach(this);
                    Thread t = new Thread(auth);
                    t.start();
                }else {
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
            case ACTIVITY_SELECT_MULTIPLE_NOTIFY:
            {
                //in week view, if multiple activities were recorded in the same day, they appear as one day in the week. If clicked it will open a new list with each ride
                ArrayList<RideOverview> multiRides = (ArrayList<RideOverview>)e.getEventArgs()[0];
                RecentActivityAdapter adapter = new RecentActivityAdapter(multiRides);
                adapter.attach(this);
                recyclerView.setAdapter(adapter);
                break;
            }
        }
    }

}