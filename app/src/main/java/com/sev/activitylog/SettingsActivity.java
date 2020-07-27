package com.sev.activitylog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{
    private boolean dirty = false, logout = false;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        Toolbar actionBar = (Toolbar)findViewById(R.id.detailToolbar);
        setSupportActionBar(actionBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_arrow);

        findViewById(R.id.log).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        ((Spinner)findViewById(R.id.settingMeasurement)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                dirty = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ((Spinner)findViewById(R.id.settingMeasurement)).setSelection(Settings.try_getInstance().isImperial() ? 0 : 1);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            NavigationSingleton.getInstance().goBack(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onStop() {
        super.onStop();
        if(dirty){
            Settings settings = Settings.try_getInstance();
            settings.setImperial(((Spinner)findViewById(R.id.settingMeasurement)).getSelectedItemPosition() == 0);
            settings.setLoggedIn(!logout);
            settings.save(getSharedPreferences(Settings.PREFERENCES_ID, MODE_PRIVATE));
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.log:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setMessage(R.string.settings_logout_warning).setTitle(R.string.settings_logout_title).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dirty = true;
                        logout = true;
                        AuthToken.clear(getSharedPreferences("auth_token", MODE_PRIVATE));
                        Toast.makeText(SettingsActivity.this, getString(R.string.logout_confirmation), Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton(R.string.cancel, null);
                dialogBuilder.show();
                break;
            case R.id.settingMeasurement:
                dirty = true;
                break;
            case R.id.delete:
                new AlertDialog.Builder(this).setMessage(R.string.setting_delete_msg).setTitle(R.string.settings_delete_title).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        StorageModel.deleteCache(SettingsActivity.this);
                        RepeatCache.getInstance(SettingsActivity.this).deleteFile();
                        Toast.makeText(SettingsActivity.this, getString(R.string.delete_confirmation), Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton(R.string.cancel, null).show();
                break;
        }
    }

    @Override
    public void onBackPressed(){
        if(NavigationSingleton.getInstance().empty())
            moveTaskToBack(true);
        else
            NavigationSingleton.getInstance().goBack(this);
    }
}

class Settings {
    private static Settings instance;
    private final static ReentrantReadWriteLock instanceLock = new ReentrantReadWriteLock();
    private boolean loggedIn, imperial, glInstructions;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static final String PREFERENCES_ID = "settings";
    private static final double METERS_MILES_CONVERSION = 0.000621371;
    private static final double METERS_FEET_CONVERSION = 3.28084;
    private Settings(SharedPreferences prefs) {
        try {
            readWriteLock.writeLock().lock();
            loggedIn = prefs.getBoolean("log_in", false);
            imperial = prefs.getBoolean("imperial", true);
            glInstructions = prefs.getBoolean("instructions", true);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
    private Settings() {
        try {
            readWriteLock.writeLock().lock();
            loggedIn = false;
            imperial = true;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
    public void save(SharedPreferences prefs){
        SharedPreferences.Editor editor = prefs.edit();
        try{
            readWriteLock.readLock().lock();
            editor.putBoolean("log_in", loggedIn);
            editor.putBoolean("imperial", imperial);
            editor.putBoolean("instructions", glInstructions);
            editor.commit();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }
    public boolean isLoggedIn() {
        boolean log = false;
        try{
            readWriteLock.readLock().lock();
            log = loggedIn;
        } finally {
            readWriteLock.readLock().unlock();
        }
        return log;
    }

    public boolean isImperial() {
        boolean imp = false;
        try{
            readWriteLock.readLock().lock();
            imp = imperial;
        } finally {
            readWriteLock.readLock().unlock();
        }
        return imp;
    }
    public boolean showGLInstructions() {
        boolean imp = true;
        try{
            readWriteLock.readLock().lock();
            imp = glInstructions;
        } finally {
            readWriteLock.readLock().unlock();
        }
        return imp;
    }
    //Returns the factor by which to multiply the standard distance unit (meters) by to get the user's preferred unit. All measurements from strava are in meters
    public static double metersDistanceConversion() {
        return throw_getInstance().isImperial() ? METERS_MILES_CONVERSION : 1.0 / 1000;
    }
    public static double metersElevationConversion() {
        return throw_getInstance().isImperial() ? METERS_FEET_CONVERSION : 1.0;
    }
    //Returns the unit string based on the user's measurement system
    public static String speedUnits(){
        return throw_getInstance().isImperial() ? "mph" : "km/h";
    }
    public static String distanceUnits(){
        return throw_getInstance().isImperial() ? "miles" : "km";
    }
    public static String elevationUnits() {
        return throw_getInstance().isImperial() ? "ft" : "m";
    }
    public static Settings getInstance(SharedPreferences prefs) {
        instanceLock.readLock().lock();
        Settings set;
        if(instance == null) {
            instanceLock.readLock().unlock();
            instanceLock.writeLock().lock();
            instance = new Settings(prefs);
            set = instance;
            instanceLock.writeLock().unlock();
        }else{
            set = instance;
            instanceLock.readLock().unlock();
        }
        return set;
    }
    //If you want to assume that the singleton has already been initialized, you can use this function to get the instance without passing shared preferences
    //If not initializes, returns a Settings instance with default parameters
    public static Settings try_getInstance(){
        Settings settings;
        try{
            settings = throw_getInstance();
        } catch (IllegalStateException e){
            settings = new Settings();
        }
        return settings;
    }
    //If you want to assume that the singleton has already been initialized, you can use this function to get the instance without passing shared preferences
    //If not initializes, throws an exception
    public static Settings throw_getInstance(){
        Settings i;
        instanceLock.readLock().lock();
        i = instance;
        instanceLock.readLock().unlock();
        if(i == null) throw new IllegalStateException("Settings is uninitialized!");
        return i;
    }
    public void setLoggedIn(boolean l){
        instanceLock.writeLock().lock();
        loggedIn = l;
        instanceLock.writeLock().unlock();
    }
    public void setImperial(boolean imp){
        instanceLock.writeLock().lock();
        imperial = imp;
        instanceLock.writeLock().unlock();
    }
    public void setGlInstructions(boolean show){
        instanceLock.writeLock().lock();
        glInstructions = show;
        instanceLock.writeLock().unlock();
    }
}
