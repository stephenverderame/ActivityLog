package com.sev.activitylog;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.view.View.GONE;

public class DetailedActivityView extends AppCompatActivity implements Observer {

    private RideOverview rideBasics;
    private DetailedRide rideData;
    private OAuth auth;
    private AuthToken token;

    private GLView gl;
    private GLSceneComposite scene;
    private Terrain terrain;
    private GLCamera camera;
    private DirectionalLight sun;
    private GLShadowPass shadow;
    private Map route;

    private class TouchState {
        public float[] lastF1 = new float[] {-1, -1}, lastF2 = new float[] {-1, -1};
        public boolean multiTouch = false;
    }

    private TouchState t;
    private String notesFilename;
    private boolean dirtyNotes, dirtyActivity;
    private String activityNotes;
    private LinearLayout masterContainer;
    private View detailPage;
    private Future<Pos[][]> heightMap;
    private MapImage map;
    private StorageModel model;
    private boolean shouldSave;

    private TrendsFragment trends;
    private LinkedList<RideOverview> matchedRides;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        t = new TouchState();
        setContentView(R.layout.detail_master);
        masterContainer = (LinearLayout)findViewById(R.id.detailMasterContainer);
        detailPage = getLayoutInflater().inflate(R.layout.detail_view, null);
        masterContainer.addView(detailPage);
        rideBasics = (RideOverview)getIntent().getSerializableExtra("activity");
        token = getIntent().getParcelableExtra("auth_token");
        ((Button)findViewById(R.id.viewGLBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            //Switch to gl view and push state onto stack
            public void onClick(View view) {
                NavigationSingleton.getInstance().pushState(new NavigationCommand(DetailedActivityView.this, new Object[]{rideData}, new NavigationAction() { //pushes current state so we can go back in the gl view
                    @Override
                    public void navigate(AppCompatActivity fromActivity, AppCompatActivity destActivity, Object[] dstState, Object... args) {
                        if(fromActivity == destActivity){
                            masterContainer.removeAllViews();
                            masterContainer.addView(detailPage);
                            DetailedActivityView.this.notify(new ObserverEventArgs(ObserverNotifications.RIDE_DETAILS_NOTIFY, rideData));
                        }
                    }
                }));
                masterContainer.removeAllViews();
                masterContainer.addView(gl);
                Settings s = Settings.getInstance(getSharedPreferences(Settings.PREFERENCES_ID, MODE_PRIVATE));
                if(s.showGLInstructions()) {
                    new AlertDialog.Builder(DetailedActivityView.this).setMessage(R.string.gl_instructions).setNeutralButton(R.string.ok, null).setNegativeButton(R.string.got_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            s.setGlInstructions(false);
                            s.save(getSharedPreferences(Settings.PREFERENCES_ID, MODE_PRIVATE));
                        }
                    }).show();
                }
            }
        });
        dirtyNotes = false;
        dirtyActivity = false;
        Toolbar actionBar = (Toolbar)findViewById(R.id.detailToolbar);
        actionBar.inflateMenu(R.menu.top_detail_nav_menu);
        setSupportActionBar(actionBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_arrow); //creates an action bar with a menu item on the left side
        //Notes text changed
        ((EditText)findViewById(R.id.rideNotes)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                DetailedActivityView.this.dirtyNotes = true;
                textView.setOnEditorActionListener(null); //detaches listener, we already know we have data to save
                return true;
            }
        });
        //Exertion meter changed
        ((SeekBar)findViewById(R.id.exertion_meter)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                ((TextView)findViewById(R.id.exertion_label)).setText("Exertion Level: " + i);
                dirtyActivity = true;
                seekBar.getThumb().setTint(i <= 5 ? Util.rgba(i / 5.f, 1.f, 0, 1) : Util.rgba(1.0f, (10 - i) / 4.f, 0, 1));
                rideBasics.setExertion(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        ((CheckBox)findViewById(R.id.race_check)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dirtyActivity = true;
                rideBasics.setRace(b);
            }
        });
        ((CheckBox)findViewById(R.id.race_check)).setChecked(rideBasics.getRace());
        ((SeekBar)findViewById(R.id.exertion_meter)).setProgress(rideBasics.getExertion());
        model = new StorageModel(this);
        Future<DetailedRide> details = model.getRideDetails(rideBasics.getId());
        try {
            rideData = details.get();
            Log.d("Detail", "Got details from storage!");
        } catch (Exception e) {e.printStackTrace();}
        if(rideData != null){ //if data is cached
            Log.d("Detail", "Data exists!");
            rideData.setOverview(rideBasics);
            notify(new ObserverEventArgs(ObserverNotifications.RIDE_DETAILS_NOTIFY, rideData));
            shouldSave = false;
        }else { //if data is not cached
            if(token == null)
                token = new AuthToken(getSharedPreferences(AuthToken.PREFERENCES_ID, MODE_PRIVATE));
            Log.d("Detail", "Doing authentication!");
            auth = new OAuth(token);
            auth.attach(this);
            new Thread(auth).start();
            shouldSave = true;
        }
        gl = new GLView(DetailedActivityView.this);
        gl.attach(DetailedActivityView.this);
        matchedRides = new LinkedList<>();
        trends = (TrendsFragment)getSupportFragmentManager().findFragmentById(R.id.matchedRidesFragment);
        trends.setData(matchedRides);
        StorageModel data = new StorageModel(this);
        data.attach(this);
        data.getRides(0);

    }
    @Override
    public void onBackPressed(){
        if(NavigationSingleton.getInstance().empty())
            moveTaskToBack(true);
        else if(dirtyActivity)
            NavigationSingleton.getInstance().goBack(this, rideBasics);
        else
            NavigationSingleton.getInstance().goBack(this);
    }
    @Override
    public void notify(ObserverEventArgs e) {
        switch(e.getEventType()){
            case OAUTH_NOTIFY:
            {
                StravaModel model = new StravaModel(auth);
                model.attach(this);
                model.getRideDetails(rideBasics.getId());
                break;
            }
            case RIDE_DETAILS_NOTIFY:
            {
                ActivityView view = (ActivityView)findViewById(R.id.detailView);
                rideData = (DetailedRide)e.getEventArgs()[0];
                if(rideData != null) {
                    Log.d("Detail View", "Setting view!");
                    view.setTitle(rideBasics.getName());
                    view.setSubtitle(new SimpleDateFormat("MMM dd yyyy hh:mm a").format(rideBasics.getDate()));
                    view.setInfoGrid(10, 3);
                    view.setInfo("Distance", String.format("%.2f %s", rideBasics.getDistance() * Settings.metersDistanceConversion(), Settings.distanceUnits()), 0, 0);
                    view.setInfo("Moving Time", TimeSpan.fromSeconds((long) rideBasics.getMovingTime()), 0, 1);
                    view.setInfo("Elevation", String.format("%.2f %s", rideBasics.getClimbed() * Settings.metersElevationConversion(), Settings.elevationUnits()), 0, 2);
                    view.setInfo("Gear", rideData.getGearName(), 1, 0);
                    view.setInfo("Average Speed", String.format("%.1f %s", rideBasics.getAverageSpeed() * Settings.metersDistanceConversion() * 3600.0, Settings.speedUnits()), 1, 1);
                    view.setInfo("Average Power", String.format("%.2f W", rideBasics.getPower()), 1, 2);
                    view.setInfo("Total Time", TimeSpan.fromSeconds((long) rideBasics.getTotalTime()), 2, 0);
                    view.setInfo("Activity", rideBasics.getActivityType(), 2, 1);
                    view.setFont(new ActivityViewFontBuilder().labelSize(16).infoSize(12).build());
                    view.invalidate();
                    ArrayList<Pos> route;
                    if((route = rideData.getRoute()) != null) {
                        MapBounds bounds = MapFactory.boundsFromRoute(route);
                        CacheEntry ce = RepeatCache.getInstance(this).get(bounds);
                        if (ce == null) {
                            RoadMapFactory rmap = new RoadMapFactory();
                            rmap.attach(this);
                            rmap.makeAsync(bounds);
                        } else {
                            Log.d("Map", "Here and opentopo data already in cache!");
                            heightMap = Executors.newSingleThreadExecutor().submit(new Callable<Pos[][]>() {
                                @Override
                                public Pos[][] call() throws Exception {
                                    return ce.heights;
                                }
                            });
                            notify(new ObserverEventArgs(ObserverNotifications.MAP_LOAD_NOTIFY, ce.map));
                        }
                    }else{//not a ride - no map data
                        findViewById(R.id.viewGLBtn).setVisibility(GONE);
                    }
                }
                notesFilename = (rideBasics.getId()) + "_notes.txt";
                openNotes(notesFilename);
                break;
            }
            case URI_NOTIFY:
                startActivity((Intent)e.getEventArgs()[0]);
                break;
            case MAP_LOAD_NOTIFY:
            {
                ActivityView view = (ActivityView)findViewById(R.id.detailView);
                MapImage img = (MapImage)e.getEventArgs()[0];
                RouteDecorator route = new RouteDecorator();
                route.decorate(img);
                route.setRoute(rideData.getRoute());
                MapDecorator decorator = route;
/*                if(TrailDecorator.isOffroad(img)) {
                    decorator = new TrailDecorator(this);
                    decorator.decorate(route);
                }*/
                this.route = decorator;
                view.setMap(decorator, 3, 0, 7, 3);
                view.invalidate();
                map = img;
                if(heightMap == null) {
                    heightMap = Pos.getHeightMap(120, 120, new Pos(route.mapBounds.top, route.mapBounds.left), new Pos(route.mapBounds.bottom, route.mapBounds.right), this);
                }
                break;
            }
            case OPENGL_INIT_NOTIFY:
            {
                //all opengl initialization must be in here
                scene = new GLSceneComposite();
                scene.addObject(new Skybox(new int[] {R.drawable.bluecloud_rt, R.drawable.bluecloud_lf, R.drawable.bluecloud_up, R.drawable.bluecloud_dn, R.drawable.bluecloud_ft,
                        R.drawable.bluecloud_bk}, getResources(), getResources().getInteger(R.integer.sky_shader_id)), false); //https://opengameart.org/content/cloudy-skyboxes));
                terrain = new Terrain(route, heightMap);
                terrain.scale(100f, 100f, 100f);
                terrain.translate(0, 0, 0);
                scene.addObject(terrain, false);
                sun = new DirectionalLight();
                sun.setColor(1, 1, 1);
                sun.translate(-8, 2, 0);
                sun.setFactors(0.5f, 0.5f, 0.8f);
                scene.addObject(sun, false);
                shadow = new GLShadowPass(sun, new float[] {0, 0, 0}, getResources());
                camera = new GLCamera(new float[] {0, 2, -1}, new float[]{0, 0, 0});
                gl.addRendererPass(shadow);
                gl.setRendererScene(scene);
                gl.setRendererView(camera);
                gl.requestRender();
//                gl.setDrawLogic(this);
                break;
            }
            case TOUCH_NOTIFY:
                onGLTouch((MotionEvent)e.getEventArgs()[0]);
                break;
            case RIDES_LOAD_PARTIAL_NOTIFY:
            {
                LinkedList<RideOverview> rides = (LinkedList<RideOverview>)e.getEventArgs()[0];
                for(RideOverview r : rides){
                    if(Math.abs(r.getDistance() - rideBasics.getDistance()) < 500
                    && Math.abs(r.getClimbed() - rideBasics.getClimbed()) < 100){
                        matchedRides.add(r);
                    }
                }
                break;
            }
            case RIDES_LOAD_NOTIFY:
                trends.notifyDataFinish();
                break;
            case REDRAW_NOTIFY:
                gl.requestRender();
                break;
        }
    }

    /**
     * Read saved notes and load them into the text view
     * @param filename
     */
    private void openNotes(String filename) {
        if(activityNotes == null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(filename)))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null)
                    builder.append(line);
                ((EditText)findViewById(R.id.rideNotes)).setText(builder.toString());
                activityNotes = builder.toString();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void saveNotes(){
        if(notesFilename != null) {
            try (PrintWriter out = new PrintWriter((openFileOutput(notesFilename, Context.MODE_PRIVATE)))) {
                out.write(((EditText)findViewById(R.id.rideNotes)).getText().toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy(){
        gl.destructor();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if(dirtyNotes) saveNotes();
        if(shouldSave && rideData != null) {
            model.saveDetailedRide(rideData);
            token.save();
        }
        if(heightMap != null && map != null){
            RepeatCache.getInstance(this).save(map, heightMap);
            RepeatCache.getInstance(this).serialize();
        }
        super.onStop();
    }

    @Override
    //Top nav bar touch
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            if(dirtyActivity)
                NavigationSingleton.getInstance().goBack(this, rideBasics);
            else
                NavigationSingleton.getInstance().goBack(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //Camera controls for opengl view. One finger -> translate. Two fingers (one static in bottom left) -> rotate. Pinch -> zoom
    private void onGLTouch(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
            {
                if(e.getPointerCount() == 1 && !t.multiTouch) {
                    if(t.lastF1[0] >= 0 && t.lastF1[1] >= 0) {
                        float dx = e.getX(0) - t.lastF1[0];
                        float dy = e.getY(0) - t.lastF1[1];
                        camera.translate(dx * 0.005f, 0, dy * 0.005f);
                    }
                    t.lastF1[0] = e.getX(0);
                    t.lastF1[1] = e.getY(0);
                }
                else if(e.getPointerCount() == 2 && t.multiTouch){
                    float[] f1 = new float[] {e.getX(0), e.getY(0)};
                    float[] f2 = new float[] {e.getX(1), e.getY(1)};
                    float threshold = 2.5f;//(float)Math.sqrt(gl.getWidth() * gl.getWidth() + gl.getHeight() * gl.getHeight()) * 0.005f;
                    if(GLM.dist(f1, t.lastF1) <= threshold && GLM.dist(f2, t.lastF2) >= threshold){
                        float dx = f2[0] - t.lastF2[0];
                        float dy = f2[1] - t.lastF2[1];
                        if(f1[0] < gl.getWidth() / 3.f && f1[1] > gl.getHeight() * 2.f / 3.f)
                            camera.rotateLook(camera.getYaw() + dx * 0.005f, camera.getPitch() + dy * 0.005f);
                    }else if(GLM.dist(f1, t.lastF1) >= threshold && GLM.dist(f2, t.lastF2) <= threshold){
                        float dx = f1[0] - t.lastF1[0];
                        float dy = f1[1] - t.lastF1[1];
                        if(f2[0] < gl.getWidth() / 3.f && f2[1] > gl.getHeight() * 2.f / 3.f)
                            camera.rotateLook(camera.getYaw() + dx * 0.005f, camera.getPitch() + dy * 0.005f);
                    } else if(GLM.dist(f1, t.lastF1) > threshold && GLM.dist(f2, t.lastF2) > threshold){
                        float dt = GLM.dist(f1, f2) - GLM.dist(t.lastF1, t.lastF2);
                            if((camera.getPos()[1] < 10f && dt < 0 ) || (camera.getPos()[1] > 2f && dt > 0)){
                                float[] p = camera.getPos();
                                camera.setPos(p[0], p[1] - dt * 0.005f, p[2]);
                                if(camera.getPos()[1] < 2) camera.setPos(p[0], 2, p[2]);
                                else if(camera.getPos()[1] > 10) camera.setPos(p[0], 10, p[2]);
                            }else{
                                camera.setFov(camera.getFov() - dt * 0.01f);
                            }
                    }
                    t.lastF2 = f2;
                    t.lastF1 = f1;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                if(e.getActionIndex() == 1) {
                    t.lastF2 = new float[]{-1, -1};
                    t.multiTouch = false;
                }
                break;
            case MotionEvent.ACTION_UP: //gesture stopped
                t.lastF1 = new float[] {-1, -1};
                t.lastF2 = new float[] {-1, -1};
                t.multiTouch = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: //finger added to gesture
                if(e.getActionIndex() == 1){
                    t.lastF2 = new float[] {e.getX(1), e.getY(1)};
                    t.multiTouch = true;
                }
                break;
            case MotionEvent.ACTION_DOWN: //gesture started
                t.lastF1 = new float[] {e.getX(), e.getY()};
                break;
            default:
                return;
        }
        gl.requestRender();
    }
}