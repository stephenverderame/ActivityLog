package com.sev.activitylog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class MapImage {
    protected Bitmap img;
    protected MapBounds mapBounds;
    public void draw(Canvas canvas, Rect destination, Paint paint){
        if(img != null)
            canvas.drawBitmap(img, new Rect(0, 0, img.getWidth(), img.getHeight()), destination, paint);
    }
    public void setMapBounds(MapBounds b){
        mapBounds = b;
    }
}
class MapBounds{
    public double left = 180, right = -180, top = -90, bottom = 90;
}
abstract class MapDecorator extends MapImage {
    protected MapImage component;
    void decorate(MapImage layer){
        component = layer;
    }
}
abstract class MapFactory implements Subject{
    protected LinkedList<Observer> observers;
    private ExecutorService executor;
    public MapFactory(){
        observers = new LinkedList<Observer>();
        executor = Executors.newSingleThreadExecutor();
    }
    protected abstract MapImage make_(MapBounds bounds);
    public MapBounds boundsFromRoute(ArrayList<Pos> route){
        MapBounds bounds = new MapBounds();
        for(Pos p : route){
            if(p.lon < bounds.left) bounds.left = p.lon;
            if(p.lon > bounds.right) bounds.right = p.lon;
            if(p.lat < bounds.bottom) bounds.bottom = p.lat;
            if(p.lat > bounds.top) bounds.top = p.lat;
        }
        return bounds;
    }
    public void makeAsync(final MapBounds bounds){
        executor.submit(new Runnable() {
            @Override
            public void run() {
                MapImage map = make_(bounds);
                ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.MAP_LOAD_NOTIFY, map));
            }
        });
    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
}
class RoadMapFactory extends MapFactory {

    private static final String API_ID = "eue3ZmJeRloe49N4jGod";
    private static final String API_CODE = "7GI8j3yr8Mn3vr_RvnHOLA";
    @Override
    protected MapImage make_(MapBounds bounds) {
        MapImage map = new MapImage();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://image.maps.api.here.com/mia/1.6/mapview").append("?app_id=").append(API_ID)
                .append("&app_code=").append(API_CODE).append("&bbox=").append(bounds.top).append(',')
                .append(bounds.left).append(',').append(bounds.right).append(',').append(bounds.bottom);
        try {
            URL url = new URL(urlBuilder.toString());
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(false);
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "image/*");
            con.setRequestProperty("Host", "image.maps.api.here.com");
            con.connect();
            map.img = BitmapFactory.decodeStream(con.getInputStream());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}

