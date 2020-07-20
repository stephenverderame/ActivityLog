package com.sev.activitylog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceControl;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

//Stores query information to fetch the appropriate data from strava
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
    private static final int TRENDS_PAGE = 4, MULTI_PAGE = 3, STATS_PAGE = 2, WEEK_PAGE = 1, RECENT_PAGE = 0;
    private int lastPage = RECENT_PAGE;

    private List<RideOverview> rideList;
    private StorageModel storage;
    private StravaModel remoteModel;
    long lastSync = 0;
    private OAuth auth;

    private boolean dataDirty = false;
    private boolean finishedLoading = false;


    private Future<ArrayList<Gear>> gearList;
    private AtomicBoolean isGearListIncomplete;

    private LinkedList<RemoteQuery> queryQ;

    private DataFragment[] fragments;

    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = Settings.getInstance(getSharedPreferences(Settings.PREFERENCES_ID, MODE_PRIVATE));
        auth = new OAuth(new AuthToken(getSharedPreferences(AuthToken.PREFERENCES_ID, MODE_PRIVATE)));
        auth.attach(this);
        remoteModel = new StravaModel(auth);
        remoteModel.attach(this);
        queryQ = new LinkedList<>();
        rideList = (List<RideOverview>)getIntent().getSerializableExtra("ride_list");
        setContentView(R.layout.activity_main);
        storage = new StorageModel(this);
        storage.attach(this);
        isGearListIncomplete = new AtomicBoolean(false);
        if(rideList == null){
            rideList = new LinkedList<>();
            storage.getRides(0);
//            notify(new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, null, null));
        }
        else{
            LinearLayout layout = (LinearLayout) findViewById(R.id.container);
            layout.removeView(findViewById(R.id.loadingBar));
            layout.removeView(findViewById(R.id.loadingText));
            gearList = storage.getGear(isGearListIncomplete);
            finishedLoading = true;
        }
        fragments = new DataFragment[5];
        fragments[RECENT_PAGE] = new RecentFragment(rideList, this);
        fragments[WEEK_PAGE] = new WeekFragment(rideList, this);
        fragments[STATS_PAGE] = new StatsFragment(rideList);
        fragments[MULTI_PAGE] = new RecentFragment(null, this);
        fragments[TRENDS_PAGE] = new TrendsFragment(rideList, this);

        Parcelable recent;
        if((recent = getIntent().getParcelableExtra("recent_state")) != null){
            fragments[RECENT_PAGE].loadStateFromParcel(recent);
            fragments[WEEK_PAGE].loadStateFromParcel(getIntent().getParcelableExtra("week_state"));
            fragments[STATS_PAGE].loadStateFromParcel(getIntent().getParcelableExtra("stat_state"));
            fragments[MULTI_PAGE].loadStateFromParcel(getIntent().getParcelableExtra("multi_state"));
            fragments[TRENDS_PAGE].loadStateFromParcel(getIntent().getParcelableExtra("graph_state"));
            lastPage = getIntent().getIntExtra("last_page", RECENT_PAGE);
            finishedLoading = true;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_fragment_container, fragments[lastPage]);
        transaction.commit();
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.navBar);
        nav.setSelectedItemId(menuIdFromPageId(lastPage));
        nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                ((DataFragment)getSupportFragmentManager().findFragmentById(R.id.main_fragment_container)).cacheState();
                switch(menuItem.getItemId()) {
                    case R.id.recentMenuItem:
                    {
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.main_fragment_container, fragments[RECENT_PAGE]);
                        transaction.commit();
                        lastPage = RECENT_PAGE;
                        break;
                    }
                    case R.id.weekMenuItem:
                        if(finishedLoading) {
                            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                            trans.replace(R.id.main_fragment_container, fragments[WEEK_PAGE]);
                            trans.commit();
                            lastPage = WEEK_PAGE;
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "Please wait until data has finished loading", Toast.LENGTH_LONG).show();
                            return false;
                        }
                        break;
                    case R.id.statMenuItem:
                    {
                        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                        trans.replace(R.id.main_fragment_container, fragments[STATS_PAGE]);
                        trans.commit();
                        lastPage = STATS_PAGE;
                        break;
                    }
                    case R.id.graphMenuItem:
                    {
                        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                        trans.replace(R.id.main_fragment_container, fragments[TRENDS_PAGE]);
                        trans.commit();
                        lastPage = TRENDS_PAGE;
                        break;
                    }
                    default:
                        return false;
                }
                ((FilterFragment)getSupportFragmentManager().findFragmentById(R.id.filterView)).clear();
                return true;
            }
        });
        Toolbar toolbar = (Toolbar)findViewById(R.id.detailToolbar);
        toolbar.inflateMenu(R.menu.top_detail_nav_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.refresh_btn){
                    new AlertDialog.Builder(MainActivity.this).setMessage(R.string.sync_msg).setTitle(R.string.sync_title).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            loadFromRemote(RemoteQuery.RIDE_QUERY & RemoteQuery.GEAR_QUERY, lastSync);
                        }
                    }).setNegativeButton(R.string.cancel, null).show();
                    return true;
                }else if(item.getItemId() == R.id.settings_btn){
                    NavigationSingleton.getInstance().pushState(new NavigationCommand(MainActivity.this, new Object[]{rideList, fragments[RECENT_PAGE].getState(), fragments[WEEK_PAGE].getState(),
                            fragments[STATS_PAGE].getState(), fragments[MULTI_PAGE].getState(), fragments[TRENDS_PAGE].getState(), lastPage}, MainActivity.this::navigate));
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));

                }
                return false;
            }
        });
        ((FilterFragment)getSupportFragmentManager().findFragmentById(R.id.filterView)).setOnSearchListener(this);
        ((FilterFragment)getSupportFragmentManager().findFragmentById(R.id.filterView)).setGearList(gearList);
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
                    for(int i = 0; i < fragments.length; ++i)
                        fragments[i].notifyDataFinish();
                    finishedLoading = true;
                    try {
                        gearList = storage.getGear(isGearListIncomplete);
                        ArrayList<Gear> gears = gearList.get();
                        if(gears == null || gears.size() == 0 || isGearListIncomplete.get()){
                            Log.d("Main", "Need update gear");
                            loadFromRemote(RemoteQuery.GEAR_QUERY, 0);
                        }else{
                            Log.d("Main", "Gear is fine");
                            ((FilterFragment)getSupportFragmentManager().findFragmentById(R.id.filterView)).setGearList(gearList);
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
                        ((DataFragment)getSupportFragmentManager().findFragmentById(R.id.main_fragment_container)).notifyDataInsertion(0, newRides.size());
                    }
                    else{
                        rideList.addAll(newRides);
                        ((DataFragment)getSupportFragmentManager().findFragmentById(R.id.main_fragment_container)).notifyDataInsertion(rideList.size() - newRides.size(), newRides.size());
                    }
                }
                break;
            }
            case ACTIVITY_SELECT_NOTIFY:
            {
                LinkedList<RideOverview> list = new LinkedList<>();
                list.addAll(rideList);
                NavigationSingleton.getInstance().pushState(new NavigationCommand(this, new Object[]{list, fragments[RECENT_PAGE].getState(), fragments[WEEK_PAGE].getState(),
                        fragments[STATS_PAGE].getState(), fragments[MULTI_PAGE].getState(), fragments[TRENDS_PAGE].getState(), lastPage}, this));
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
                fragments[MULTI_PAGE].setData((LinkedList<RideOverview>)e.getEventArgs()[0]);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.main_fragment_container, fragments[MULTI_PAGE]);
                ft.commit();
                lastPage = MULTI_PAGE;
                break;
            }
        }
    }

    @Override
    public void navigate(AppCompatActivity fromActivity, AppCompatActivity destActivity, Object[] dstState, Object... args) {
        if(destActivity == fromActivity){ //if navigating from this activity to another state in this activity
/*            RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = (RecyclerView.Adapter<RecyclerView.ViewHolder>)dstState[0];
            Parcelable adapterState = (Parcelable)dstState[1];
            recyclerView.setAdapter(adapter);
            if(adapterState != null) recyclerView.getLayoutManager().onRestoreInstanceState(adapterState);*/

        }else{
            Intent mainIntent = new Intent(fromActivity, destActivity.getClass());
            LinkedList<RideOverview> rideList = new LinkedList<>(); rideList.addAll(this.rideList);//(LinkedList<RideOverview>)dstState[0];
            if(args.length >= 1){
                RideOverview editedRide = (RideOverview)args[0];
                int i = RideOverview.indexOfExact(rideList, editedRide.getDate());
                if(i != -1) rideList.set(i, editedRide);
            }
            mainIntent.putExtra("ride_list", rideList);
            mainIntent.putExtra("recent_state", (Parcelable)dstState[1]);
            mainIntent.putExtra("week_state", (Parcelable)dstState[2]);
            mainIntent.putExtra("stat_state", (Parcelable)dstState[3]);
            mainIntent.putExtra("multi_state", (Parcelable)dstState[4]);
            mainIntent.putExtra("graph_state", (Parcelable)dstState[5]);
            mainIntent.putExtra("last_page", (Integer)dstState[6]);
            startActivity(mainIntent);
        }
    }

    @Override
    public void search(SearchFilters filter) {
        DataFragment activeFrag =  (DataFragment)getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        activeFrag.filter(filter);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.detach(activeFrag).attach(activeFrag);
       transaction.commit();
    }
    private void loadFromRemote(int dataType, long lastSyncTime){
        Log.d("Remote", "Loading " + dataType);
        if(settings.isLoggedIn()) {
            synchronized (queryQ) {
                queryQ.push(new RemoteQuery(dataType, lastSyncTime));
                Log.d("Remote", "Pushing request");
            }
            if (!(auth.isAuthComplete() || auth.isAuthenticating())) {
                Thread producer = new Thread(auth);
                producer.start();
            } else if (!auth.isAuthenticating()) {
                continueFromRemote();
            }
        }else{
            new AlertDialog.Builder(this).setTitle(R.string.login_title).setMessage(R.string.login_msg).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    settings.setLoggedIn(true);
                    settings.save(MainActivity.this.getSharedPreferences("settings", MODE_PRIVATE));
                    loadFromRemote(dataType, lastSyncTime);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.notify(new ObserverEventArgs(ObserverNotifications.RIDES_LOAD_NOTIFY, System.currentTimeMillis()));
                }
            }).show();
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
                        ((FilterFragment)getSupportFragmentManager().findFragmentById(R.id.filterView)).setGearList(gearList);
                    });
                }
                queryQ.pop();
            }
        }
    }
    private int menuIdFromPageId(int pageId){
        switch(pageId){
            case WEEK_PAGE:
                return R.id.weekMenuItem;
            case STATS_PAGE:
                return R.id.statMenuItem;
            case TRENDS_PAGE:
                return R.id.graphMenuItem;
            default:
                return R.id.recentMenuItem;
        }
    }
}