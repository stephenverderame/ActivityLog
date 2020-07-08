package com.sev.activitylog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class RemoteQuery {
    public static final int RIDE_QUERY = 1, GEAR_QUERY = 2;
    public RemoteQuery(int queryType, long syncTime){
        this.queryType = queryType;
        this.syncTime = syncTime;
    }
    int queryType;
    long syncTime;
}
public class MainActivity extends AppCompatActivity implements Observer, NavigationAction, onSearchListener {

    private List<RideOverview> rideList;
    private StorageModel storage;
    private StravaModel remoteModel;
    long lastSync = 0;
    private OAuth auth;
    private RecyclerView recyclerView;
    private RecentActivityAdapter recentActivityViewAdapter; //adapter for per activity view
    private WeekViewAdapter recyclerWeekAdapter; //adapter for per week view

    private boolean dataDirty = false, gearDirty = false;
    private boolean finishedLoading = false;

    private NavigationCommand[] menuNav;

    private Future<ArrayList<Gear>> gearList;
    private AtomicBoolean isGearListIncomplete;

    private LinkedList<RemoteQuery> queryQ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new OAuth(new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE)));
        auth.attach(this);
        remoteModel = new StravaModel(auth);
        remoteModel.attach(this);
        queryQ = new LinkedList<>();
        menuNav = new NavigationCommand[2];
        for(int i = 0; i < menuNav.length; ++i)
            menuNav[i] = new NavigationCommand(this, this);
        boolean needToLoad = true;
        rideList = (List<RideOverview>)getIntent().getSerializableExtra("ride_list");
        setContentView(R.layout.activity_main);
        storage = new StorageModel(this);
        storage.attach(this);
        isGearListIncomplete = new AtomicBoolean(false);
        if(rideList == null){
            rideList = new LinkedList<>();
            storage.getRides(0);
        }
        else{
            LinearLayout layout = (LinearLayout) findViewById(R.id.container);
            layout.removeView(findViewById(R.id.loadingBar));
            layout.removeView(findViewById(R.id.loadingText));
        }
 //       notify(new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, null, null)); //DEBUGGING - to force update from strava
        recyclerView = (RecyclerView)findViewById(R.id.recycle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivityViewAdapter = new RecentActivityAdapter(rideList);
        recentActivityViewAdapter.attach(this);
        recyclerView.setAdapter(recentActivityViewAdapter);
        recyclerWeekAdapter = new WeekViewAdapter(rideList);
        recyclerWeekAdapter.attach(this);
        menuNav[0].setDestState(new Object[] {recentActivityViewAdapter, getIntent().getParcelableExtra("recent_state")});
        menuNav[1].setDestState(new Object[] {recyclerWeekAdapter, getIntent().getParcelableExtra("week_state")});
        menuNav[0].goTo(this);

        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.navBar);
        nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.recentMenuItem:
                        menuNav[1].setDestState(getPartialMemento());
                        menuNav[0].goTo(MainActivity.this);
                        break;
                    case R.id.weekMenuItem:
                        if(finishedLoading) {
                            menuNav[0].setDestState(getPartialMemento());
                            menuNav[1].goTo(MainActivity.this);
                        }
                        else
                            Toast.makeText(getApplicationContext(), "Please wait until data has finished loading", Toast.LENGTH_LONG);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });
        Toolbar toolbar = (Toolbar)findViewById(R.id.detailToolbar);
        toolbar.inflateMenu(R.menu.top_detail_nav_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.refresh_btn){
                    loadFromRemote(RemoteQuery.RIDE_QUERY & RemoteQuery.GEAR_QUERY, lastSync);
                    return true;
                }
                return false;
            }
        });
        ((FilterView)findViewById(R.id.filterView)).setOnSearchListener(this);
        ((FilterView)findViewById(R.id.filterView)).setGearList(gearList);
    }

    @Override
    public void notify(ObserverEventArgs e) {
        switch (e.getEventType()) {
            case OAUTH_NOTIFY: {
                continueFromRemote();
                break;
            }
            case URI_NOTIFY:
                startActivity((Intent) e.getEventArgs()[0]);
                break;
            case RIDES_LOAD_NOTIFY: { //flagged by both storage and remote models. So this code can be run multiple times if the local storage is out of date
                if(e.getEventArgs()[0] != null)
                    lastSync = (long) e.getEventArgs()[0];
                if(System.currentTimeMillis() - lastSync > 1000 * 3600 * 24){ //data outdated, get new data from remote server
                    dataDirty = true;
                    loadFromRemote(RemoteQuery.RIDE_QUERY, lastSync);
                }else {
                    recyclerWeekAdapter.init();
                    finishedLoading = true;
                    try {
                        gearList = storage.getGear(isGearListIncomplete);
                        ArrayList<Gear> gears = gearList.get();
                        if(gears == null || gears.size() == 0 || isGearListIncomplete.get()){
                            Log.d("Main", "Need update gear");
                            loadFromRemote(RemoteQuery.GEAR_QUERY, 0);
                        }else{
                            Log.d("Main", "Gear is fine");
                        }
                    } catch (Exception ex) {ex.printStackTrace();}
                }
                if (dataDirty){
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
                        recentActivityViewAdapter.notifyItemRangeInserted(0, newRides.size());
                        recyclerView.scrollToPosition(0);
                    }
                    else{
                        rideList.addAll(newRides);
                        recentActivityViewAdapter.notifyItemRangeChanged(rideList.size() - newRides.size(), newRides.size());
                    }
                }
                break;
            }
            case ACTIVITY_SELECT_NOTIFY:
            {
                LinkedList<RideOverview> list = new LinkedList<>();
                list.addAll(rideList);
                NavigationSingleton.getInstance().pushState(new NavigationCommand(this, new Object[]{list, menuNav[0].getState()[1], menuNav[1].getState()[1]}, this));
                Intent detailedIntent = new Intent(this, DetailedActivityView.class);
                detailedIntent.putExtra("activity", (RideOverview)e.getEventArgs()[0]);
                if(auth != null)
                    detailedIntent.putExtra("auth_token", auth.getAuthToken());
                startActivity(detailedIntent);
                break;
            }
            case ACTIVITY_SELECT_MULTIPLE_NOTIFY:
            {
                //in week view, if multiple activities were recorded in the same day, they appear as one day in the week. If clicked it will open a new list with each ride
                LinkedList<RideOverview> multiRides = (LinkedList<RideOverview>)e.getEventArgs()[0];
                RecentActivityAdapter adapter = new RecentActivityAdapter(multiRides);
                adapter.attach(this);
                recyclerView.setAdapter(adapter);
                break;
            }
        }
    }

    @Override
    public void navigate(AppCompatActivity fromActivity, AppCompatActivity destActivity, Object[] dstState) {
        if(destActivity == fromActivity){ //if navigating from this activity to another state in this activity
            RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = (RecyclerView.Adapter<RecyclerView.ViewHolder>)dstState[0];
            Parcelable adapterState = (Parcelable)dstState[1];
            recyclerView.setAdapter(adapter);
            if(adapterState != null) recyclerView.getLayoutManager().onRestoreInstanceState(adapterState);

        }else{
            Intent mainIntent = new Intent(fromActivity, destActivity.getClass());
            mainIntent.putExtra("ride_list", (LinkedList<RideOverview>)dstState[0]);
            mainIntent.putExtra("recent_state", (Parcelable)dstState[1]);
            mainIntent.putExtra("week_state", (Parcelable)dstState[2]);
            startActivity(mainIntent);
        }
    }
    private Object[] getPartialMemento(){
        return new Object[] {recyclerView.getAdapter(), recyclerView.getLayoutManager().onSaveInstanceState()};
    }

    @Override
    public void search(SearchFilters filter) {
        LinkedList<RideOverview> rides = new LinkedList<>();
        for(RideOverview r : rideList) {
            if (r.doesApply(filter)) rides.add(r);
        }
        recyclerView.setAdapter(new RecentActivityAdapter(rides));
    }
    @Override
    public void onStop(){
        if(gearDirty) {
            try {
                storage.saveGearList(gearList.get(3, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
        super.onStop();
    }
    private void loadFromRemote(int dataType, long lastSyncTime){
        Log.d("Remote", "Loading " + dataType);
        synchronized (queryQ) {
            queryQ.push(new RemoteQuery(dataType, lastSyncTime));
            Log.d("Remot", "Pushing request");
        }
        if(!(auth.isAuthComplete() || auth.isAuthenticating())) {
            Thread producer = new Thread(auth);
            producer.start();
        }else if(auth.isAuthenticating()){
            Thread consumer = new Thread(new Runnable() {
                @Override
                public void run() {
                    do{
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while(auth.isAuthenticating());
                    continueFromRemote();
                }
            });
            consumer.start();
        }
        else{
            continueFromRemote();
        }
    }
    private void continueFromRemote(){
        Log.d("Main", "Continuing remote request");
        synchronized (queryQ) {
            while (!queryQ.isEmpty()) {
                RemoteQuery q = queryQ.peek();
                if ((q.queryType & RemoteQuery.RIDE_QUERY) != 0) {
                    remoteModel.getRides(q.syncTime);
                }
                if ((q.queryType & RemoteQuery.GEAR_QUERY) != 0) {
                    Log.d("Main", "Getting gear list!");
                    try {
                        remoteModel.addGearIds(gearList.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    gearList = remoteModel.getGear(isGearListIncomplete);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        ((FilterView) findViewById(R.id.filterView)).setGearList(gearList);
                    });
                }
                queryQ.pop();
            }
        }
    }
}