package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements Observer {

    private Future<LinkedList<RideOverview>> rideList;
    private Timer modelTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OAuth auth = new OAuth();
        auth.attach(this);
        auth.setPreferences(getPreferences(MODE_PRIVATE));
        Thread t = new Thread(auth);
        t.start();
    }

    @Override
    public void notify(ObserverEventArgs e) {
        switch(e.getEventType()){
            case OAUTH_NOTIFY:
            {
                OAuth auth = (OAuth)e.getEventArgs()[0];
                if(auth.isAuthComplete()){
                    auth.save(getPreferences(MODE_PRIVATE));
                    StravaModel model = new StravaModel(auth);
                    rideList = model.getRides();
                    modelTimer = new Timer();
                    modelTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            new Handler(Looper.getMainLooper()).post(new Runnable(){ //Posts a runnable to be run on the main thread. Android only
                                @Override
                                public void run() {
                                    populate();
                                }
                            });
                        }
                    }, 0, 500);
                }
                break;

            }
            case URI_NOTIFY:
                startActivity((Intent)e.getEventArgs()[0]);
                break;
        }
    }
    public void populate(){
        if(rideList != null && rideList.isDone()){
            modelTimer.cancel();
            try {
                LinkedList<RideOverview> rides = rideList.get();
                LinearLayout layout = (LinearLayout)findViewById(R.id.container);
                for(RideOverview ride : rides){
                    ActivityView view = new ActivityView(this);
                    view.setTitle(ride.getName());
                    Date d = ride.getDate();
                    view.setSubtitle(new SimpleDateFormat("MMM dd yyyy hh:mm a").format(ride.getDate()));
                    view.setInfoGrid(1, 3);
                    view.setInfo("Distance", String.format("%.2f miles", ride.getDistance() * 0.000621371), 0, 0);
                    view.setInfo("Moving Time", TimeSpan.fromSeconds((long)ride.getMovingTime()), 0, 1);
                    view.setInfo("Elevation", String.format("%.2f ft", ride.getClimbed() * 3.28084), 0, 2);
                    view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(getResources().getDisplayMetrics().density * 100)));
                    float dp = getResources().getDisplayMetrics().density;
                    view.setPadding(new PaddingBuilder().top(20).left(20).right(20).build());
                    view.setFont(new ActivityViewFontBuilder().titleSize(18).subtitleSize(8).labelSize(10).infoSize(6).build());
                    view.setInfoPadding(5);
                    layout.addView(view);

                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}