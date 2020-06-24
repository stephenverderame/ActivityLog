package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;

public class DetailedActivityView extends AppCompatActivity implements Observer {

    private long ID;
    private DetailedRide rideData;
    private OAuth auth;
    private AuthToken token;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_view);
        ID = getIntent().getLongExtra("activity_id", -1);
        token = getIntent().getParcelableExtra("auth_token");
        if(token == null)
            token = new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE));
        auth = new OAuth(token);
        auth.attach(this);
        new Thread(auth).start();

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
                DetailedRide ride = (DetailedRide)e.getEventArgs()[0];
                if(ride != null) {
                    Log.d("Detail View", "Setting view!");
                    view.setTitle(ride.getOverview().getName());
                    view.setSubtitle(new SimpleDateFormat("MMM dd yyyy hh:mm a").format(ride.getOverview().getDate()));
                    view.setInfoGrid(5, 3);
                    view.setInfo("Distance", String.format("%.2f miles", ride.getOverview().getDistance() * 0.000621371), 0, 0);
                    view.setInfo("Moving Time", TimeSpan.fromSeconds((long) ride.getOverview().getMovingTime()), 0, 1);
                    view.setInfo("Elevation", String.format("%.2f ft", ride.getOverview().getClimbed() * 3.28084), 0, 2);
                    view.setInfo("Gear", ride.getGearName(), 1, 0);
                    view.setInfo("Average Speed", String.format("%.1f mph", ride.getOverview().getAverageSpeed() * 0.000621371 / 3600.0), 1, 1);
                    view.setInfo("Average Power", String.format("%.2f W", ride.getOverview().getPower()), 1, 2);
                    view.setInfo("Total Time", TimeSpan.fromSeconds((long) ride.getOverview().getTotalTime()), 2, 0);
                    view.setInfo("Activity", ride.getOverview().getActivityType(), 2, 1);
                    view.setFont(new ActivityViewFontBuilder().labelSize(10).infoSize(6).build());
                    view.invalidate();
                }
                break;
            }
            case URI_NOTIFY:
                startActivity((Intent)e.getEventArgs()[0]);
                break;
        }
    }
}