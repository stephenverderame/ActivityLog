package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class DetailedActivityView extends AppCompatActivity implements Observer {

    private long ID;
    private DetailedRide rideData;
    private OAuth auth;
    private AuthToken token;

    private GLView gl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.detail_view);
/*        ID = getIntent().getLongExtra("activity_id", -1);
        token = getIntent().getParcelableExtra("auth_token");
        if(token == null)
            token = new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE));
        auth = new OAuth(token);
        auth.attach(this);
        new Thread(auth).start();*/

        gl = new GLView(this);
        setContentView(gl);
        Timer t = new Timer();
/*        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                gl.requestRender();
            }
        }, 0, 100);*/

    }

    @Override
    public void notify(ObserverEventArgs e) {
        switch(e.getEventType()){
            case OAUTH_NOTIFY:
            {
                StravaModel model = new StravaModel(auth);
                model.attach(this);
                model.getRideDetails(ID);
                break;
            }
            case RIDE_DETAILS_NOTIFY:
            {
                ActivityView view = (ActivityView)findViewById(R.id.detailView);
                rideData = (DetailedRide)e.getEventArgs()[0];
                if(rideData != null) {
                    Log.d("Detail View", "Setting view!");
                    view.setTitle(rideData.getOverview().getName());
                    view.setSubtitle(new SimpleDateFormat("MMM dd yyyy hh:mm a").format(rideData.getOverview().getDate()));
                    view.setInfoGrid(10, 3);
                    view.setInfo("Distance", String.format("%.2f miles", rideData.getOverview().getDistance() * 0.000621371), 0, 0);
                    view.setInfo("Moving Time", TimeSpan.fromSeconds((long) rideData.getOverview().getMovingTime()), 0, 1);
                    view.setInfo("Elevation", String.format("%.2f ft", rideData.getOverview().getClimbed() * 3.28084), 0, 2);
                    view.setInfo("Gear", rideData.getGearName(), 1, 0);
                    view.setInfo("Average Speed", String.format("%.1f mph", rideData.getOverview().getAverageSpeed() * 0.000621371 / 3600.0), 1, 1);
                    view.setInfo("Average Power", String.format("%.2f W", rideData.getOverview().getPower()), 1, 2);
                    view.setInfo("Total Time", TimeSpan.fromSeconds((long) rideData.getOverview().getTotalTime()), 2, 0);
                    view.setInfo("Activity", rideData.getOverview().getActivityType(), 2, 1);
                    view.setFont(new ActivityViewFontBuilder().labelSize(10).infoSize(6).build());
                    view.invalidate();
                    RoadMapFactory rmap = new RoadMapFactory();
                    rmap.attach(this);
                    rmap.makeAsync(rmap.boundsFromRoute(rideData.getRoute()));
                }
                break;
            }
            case URI_NOTIFY:
                startActivity((Intent)e.getEventArgs()[0]);
                break;
            case MAP_LOAD_NOTIFY:
            {
                ActivityView view = (ActivityView)findViewById(R.id.detailView);
//                ImageView view = (ImageView)findViewById(R.id.mapView);
                MapImage img = (MapImage)e.getEventArgs()[0];
//                view.setImageBitmap(img.img);
                RouteDecorator route = new RouteDecorator();
                route.decorate(img);
                route.setRoute(rideData.getRoute());
                view.setMap(route, 3, 0, 7, 3);
                view.invalidate();
            }
        }
    }
}