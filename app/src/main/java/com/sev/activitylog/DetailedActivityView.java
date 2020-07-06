package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Timer;

public class DetailedActivityView extends AppCompatActivity implements Observer {

    private long ID;
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
    private boolean dirtyNotes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        t = new TouchState();
        setContentView(R.layout.detail_view);
        ID = getIntent().getLongExtra("activity_id", -1);
        token = getIntent().getParcelableExtra("auth_token");
        if(token == null)
            token = new AuthToken(getSharedPreferences("auth_token", MODE_PRIVATE));
        auth = new OAuth(token);
        auth.attach(this);
        new Thread(auth).start();
        ((Button)findViewById(R.id.viewGLBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gl = new GLView(DetailedActivityView.this);
                gl.attach(DetailedActivityView.this);
                setContentView(gl);
            }
        });
        dirtyNotes = false;

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
                    view.setFont(new ActivityViewFontBuilder().labelSize(16).infoSize(12).build());
                    view.invalidate();
                    RoadMapFactory rmap = new RoadMapFactory();
                    rmap.attach(this);
                    rmap.makeAsync(rmap.boundsFromRoute(rideData.getRoute()));
                    notesFilename = (rideData.getOverview().getDate().getTime() / 1000) + "_notes.txt";
                    ((EditText)findViewById(R.id.rideNotes)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                            DetailedActivityView.this.dirtyNotes = true;
                            return true;
                        }
                    });
                    openNotes(notesFilename);
                }
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
                this.route = route;
                view.setMap(route, 3, 0, 7, 3);
                view.invalidate();
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
                onTouch((MotionEvent)e.getEventArgs()[0]);
                break;
        }
    }
    private void openNotes(String filename) {
        StringBuilder notes = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(filename)))){
            String line;
            while((line = reader.readLine()) != null)
                notes.append(line).append('\n');
            ((EditText)findViewById(R.id.rideNotes)).getText().append(notes.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void saveNotes(){
        if(notesFilename != null) {
            try (PrintWriter writer = new PrintWriter(new PrintStream(openFileOutput(notesFilename, Context.MODE_PRIVATE)))) {
                String notes = ((EditText) findViewById(R.id.rideNotes)).getText().toString();
                writer.print(notes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(dirtyNotes) saveNotes();
        super.onDestroy();
    }

    private void onTouch(MotionEvent e){
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