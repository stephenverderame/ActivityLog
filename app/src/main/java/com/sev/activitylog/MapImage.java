package com.sev.activitylog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

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
abstract class Map{
    protected MapBounds mapBounds;
    protected float aspectRatio;
    public abstract void draw(Canvas canvas, Rect destination, Paint paint);

    /**
     * Finds the largest size that can fit within the given bounds while maintaining the same aspect ratio
     * @param destination intended size and position to place image
     * @return largest Rectangle that can fit in destination with the same aspect ratio
     */
    public Rect calcPreservingDestination(Rect destination){
        int newWidth = 0, newHeight = 0;
        if (aspectRatio > 1) { //width is greater, so use that as a basis for scaling
            newWidth = destination.right - destination.left;
            newHeight = Math.round(newWidth / aspectRatio);
        } else {
            newHeight = destination.bottom - destination.top;
            newWidth = Math.round(newHeight * aspectRatio);
        }
 //       if(newWidth < destination.right - destination.left) {
            destination.right = destination.left + newWidth;
            destination.bottom = destination.top + newHeight;
//        }
        return destination;
    }
    public void setAspectRatio(float r) {aspectRatio = r;}
}
public class MapImage extends Map {
    protected Bitmap img;
    public void draw(Canvas canvas, Rect destination, Paint paint) {
        if(img != null) {
            canvas.drawBitmap(img, new Rect(0, 0, img.getWidth(), img.getHeight()), destination, paint);
        }
    }
    public void setMapBounds(MapBounds b){
        mapBounds = b;
    }
    public void setImg(Bitmap map){
        img = map;
        aspectRatio = (float)img.getWidth() / img.getHeight();
    }

}
class MapBounds{
    public double left = 180, right = -180, top = -90, bottom = 90;
}
abstract class MapDecorator extends Map {
    protected Map component;
    void decorate(Map layer){

        component = layer;
        aspectRatio = component.aspectRatio;
        mapBounds = component.mapBounds;
    }
}
class RouteDecorator extends MapDecorator {
    private ArrayList<Pos> route;
    @Override
    public void draw(Canvas canvas, Rect destination, Paint paint) {
        component.draw(canvas, destination, paint);
        double lonPerX = Math.abs((double)(mapBounds.right - mapBounds.left) / (destination.right - destination.left));
        double latPerY = Math.abs((double)(mapBounds.bottom - mapBounds.top) / (destination.bottom - destination.top));
        float[] pts = new float[route.size() * 2];
        for(int i = 0; i < route.size(); ++i){
            pts[i * 2] = destination.left + (float)(Math.abs(route.get(i).lon - mapBounds.left)) / (float)lonPerX;
            pts[i * 2 + 1] = destination.top + (float)(Math.abs(mapBounds.top - route.get(i).lat)) / (float)latPerY;
        }
        for(int i = 0; i < route.size() - 1; ++i){
            canvas.drawLine(pts[i * 2], pts[i * 2 + 1], pts[(i + 1) * 2], pts[(i + 1) * 2 + 1], paint);
        }

    }
    public void setRoute(ArrayList<Pos> route){
        this.route = route;
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
    public static MapBounds boundsFromRoute(ArrayList<Pos> route){
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
    public RoadMapFactory(){
        super();
    }
    @Override
    protected MapImage make_(MapBounds bounds) {
        MapImage map = new MapImage();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://image.maps.api.here.com/mia/1.6/mapview").append("?app_id=").append(API_ID)
                .append("&app_code=").append(API_CODE).append("&bbox=").append(bounds.top).append(',')
                .append(bounds.left).append(',').append(bounds.top).append(',').append(bounds.right).append(',')
                .append(bounds.bottom).append(',').append(bounds.left).append(',').append(bounds.bottom).append(',')
                .append(bounds.right);
        try {
            URL url = new URL(urlBuilder.toString());
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(false);
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "image/*");
            con.setRequestProperty("Host", "image.maps.api.here.com");
            con.connect();
            if(con.getResponseCode() == 200) {
                Log.d("MAP", "Got image from HERE");
                map.setImg(BitmapFactory.decodeStream(con.getInputStream()));
                //gets actual viewport used
                String topRight = con.getHeaderField("Viewport-Top-Right");
                bounds.top = Double.parseDouble(topRight.substring(topRight.indexOf("Lat:") + 5, topRight.indexOf(", ")));
                bounds.right = Double.parseDouble(topRight.substring(topRight.indexOf("Lon:") + 5));
                String btmLeft = con.getHeaderField("Viewport-Bottom-Left");
                bounds.bottom = Double.parseDouble(btmLeft.substring(btmLeft.indexOf("Lat:") + 5, btmLeft.indexOf(", ")));
                bounds.left = Double.parseDouble(btmLeft.substring(btmLeft.indexOf("Lon:") + 5));
                map.setMapBounds(bounds);
            }
            else{
                Log.e("MAP", "Failed from here. Error: " + con.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            Log.e("MAP", e.toString());
        } catch (IOException e) {
            Log.e("MAP", e.toString());
        }
        return map;
    }
}

