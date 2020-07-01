package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class DetailedActivityView extends AppCompatActivity implements Observer, Runnable {

    private long ID;
    private DetailedRide rideData;
    private OAuth auth;
    private AuthToken token;

    private GLView gl;
    private GLSceneComposite scene;
    int cubeHandle;
    private Terrain terrain;
    private GLCamera camera;
    private DirectionalLight sun;
    private Map route;

    float lastX = -1, lastY = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.detail_view);
        ID = getIntent().getLongExtra("activity_id", -1);
        token = getIntent().getParcelableExtra("auth_token");
        if(token == null)
            token = new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE));
        auth = new OAuth(token);
        auth.attach(this);
        new Thread(auth).start();


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
/*                    view.setTitle(rideData.getOverview().getName());
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
                    view.invalidate();*/
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
//                ActivityView view = (ActivityView)findViewById(R.id.detailView);
                MapImage img = (MapImage)e.getEventArgs()[0];
                RouteDecorator route = new RouteDecorator();
                route.decorate(img);
                route.setRoute(rideData.getRoute());
                this.route = route;
//                view.setMap(route, 3, 0, 7, 3);
 //               view.invalidate();
                gl = new GLView(this);
                gl.attach(this);
                setContentView(gl);
                camera = new GLCamera();
                break;
            }
            case OPENGL_INIT_NOTIFY:
            {
                //all opengl initialization must be in here
                scene = new GLSceneComposite();
                scene.addObject(new Skybox(new int[] {R.drawable.bluecloud_rt, R.drawable.bluecloud_lf, R.drawable.bluecloud_up, R.drawable.bluecloud_dn, R.drawable.bluecloud_ft,
                        R.drawable.bluecloud_bk}, getResources(), getResources().getInteger(R.integer.sky_shader_id)), false); //https://opengameart.org/content/cloudy-skyboxes));
                terrain = new Terrain(route, Pos.getHeightMap(120, 120, new Pos(route.mapBounds.top, route.mapBounds.left), new Pos(route.mapBounds.bottom, route.mapBounds.right)));
                terrain.scale(100f, 100f, 100f);
                terrain.translate(0, 0, 0);
                scene.addObject(terrain, false);
                sun = new DirectionalLight();
                sun.setColor(1, 1, 1);
                sun.translate(0, 20, -20);
                sun.setFactors(0.5f, 0.5f, 0.6f);
                scene.addObject(sun, false);
 //               cubeHandle = scene.addObject(new Cube(), false);
//                ((Cube)scene.get(cubeHandle)).setColor(1.0f, 0.0f, 0.0f, 1.0f);
                gl.setRendererScene(scene);
 //               gl.setRendererView(camera);
                gl.setDrawLogic(this);
                break;
            }
            case TOUCH_NOTIFY:
                onTouch((MotionEvent)e.getEventArgs()[0]);
                break;
        }
    }

    @Override
    public void run() {
/*        GLObject cube = scene.get(cubeHandle);
        cube.clearModel();
        cube.translate(0, 0, 0);
        cube.scale(0.5f, 0.5f, 0.5f);
        cube.rotate((float)((System.currentTimeMillis() % 4000L) * 2 * Math.PI), 0, 1, 0);*/
    }

    private void onTouch(MotionEvent e){
        float x = e.getX();
        float y = e.getY();
        if(e.getAction() == MotionEvent.ACTION_MOVE && lastX != -1 && lastY != -1){
            float dx = x - lastX;
            float dy = y - lastY;
            terrain.translate(dx * 0.0005f, 0, dy * 0.0005f);
//            sun.translate(dx * 0.0005f, 0, dy * 0.0005f);
        }
        lastX = x;
        lastY = y;
    }
}