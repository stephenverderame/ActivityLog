package com.sev.activitylog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;
class SerializeableBitmap implements Serializable {
    private transient Bitmap bitmap;
    private byte[] serializedData;
    public SerializeableBitmap(Bitmap source){
        setBitmap(source);
    }
    public SerializeableBitmap(byte[] source){
        serializedData = source;
        getBitmap();
    }
    public void setBitmap(Bitmap bmp){
        bitmap = bmp;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        serializedData = stream.toByteArray();

    }
    public Bitmap getBitmap(){
        if(bitmap == null && serializedData != null){
            bitmap = BitmapFactory.decodeByteArray(serializedData, 0, serializedData.length);
        }
        return bitmap;
    }

}
abstract class Map implements Serializable {
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
public class MapImage extends Map implements Serializable {
    protected SerializeableBitmap img;
    public void draw(Canvas canvas, Rect destination, Paint paint) {
        if(img != null) {
            canvas.drawBitmap(img.getBitmap(), new Rect(0, 0, img.getBitmap().getWidth(), img.getBitmap().getHeight()), destination, paint);
        }
    }
    public void setMapBounds(MapBounds b){
        mapBounds = b;
    }
    public void setImg(Bitmap map){
        img = new SerializeableBitmap(map);
        aspectRatio = (float)img.getBitmap().getWidth() / img.getBitmap().getHeight();
    }

}
class MapBounds implements Serializable {
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
/*
class TrailDecorator extends MapDecorator {
    public class Trail {
        public String name;
        public ArrayList<Pos> points;
        public int difficulty;
        public int ID;
        public Trail() {
            points = new ArrayList<Pos>();
        }
        public Trail(JSONObject obj) throws JSONException {
            name = obj.getString("title");
            difficulty = obj.getInt("difficulty");
            ID = obj.getInt("trailid");
            points = Pos.decodePolyline(obj.getString("encodedPath"), 5);
        }
    }


    private final String API_KEY = "docs";
    private Future<ArrayList<Trail>> trails;
    private ArrayList<Trail> loadedTrails;
    private RouteDecorator internalDecorator;
    private Observer observer;
    public TrailDecorator(Observer controller) {
        internalDecorator = new RouteDecorator();
        observer = controller;
    }
    @Override
    public void draw(Canvas canvas, Rect destination, Paint paint) {
        if(loadedTrails == null && trails.isDone()) {
            try {
                loadedTrails = trails.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(loadedTrails != null){
            int old = paint.getColor();
            float oldFSize = paint.getTextSize();
            paint.setTextSize(8);
            for(Trail t : loadedTrails){
                double lonPerX = Math.abs((double)(mapBounds.right - mapBounds.left) / (destination.right - destination.left));
                double latPerY = Math.abs((double)(mapBounds.bottom - mapBounds.top) / (destination.bottom - destination.top));
                internalDecorator.setRoute(t.points);
                paint.setColor(colorFromDifficulty(t.difficulty));
                internalDecorator.draw(canvas, destination, paint);
                if(t.points.size() > 0)
                    canvas.drawText(t.name, destination.left + (float)(Math.abs(t.points.get(0).lon - mapBounds.left)) / (float)lonPerX,
                            destination.top + (float)(Math.abs(mapBounds.top - t.points.get(0).lat)) / (float)latPerY, paint);
            }
            paint.setColor(old);
            paint.setTextSize(oldFSize);
        }
        component.draw(canvas, destination, paint);
    }
    private int colorFromDifficulty(int difficulty){
        final int a = 0xFF;
        switch(difficulty){
            case 2:
                return Util.rgba_v(0xFF, 0xFF, 0xFF, a);
            case 3:
                return Util.rgba_v(0x45, 0xB4, 0x14, a);
            case 4:
            case 10:
                return Util.rgba_v(0, 0, 0, a);
            case 5:
                return Util.rgba_v(0xBE, 0, 0x14, a);
            case 6:
                return Util.rgba_v(0xAE, 0x83, 0xAE, a);
            case 7:
                return Util.rgba_v(0xFF, 0x85, 0, a);
            case 8:
            case 9:
                return Util.rgba_v(0xDC, 0x13, 0x13, a);
            default:
                return Util.rgba_v(0x85, 0x4e, 0x85, a);
        }
    }

    @Override
    void decorate(Map layer) {
        super.decorate(layer);
        internalDecorator.decorate(layer);
        trails = Executors.newSingleThreadExecutor().submit(new Callable<ArrayList<Trail>>() {
            @Override
            public ArrayList<Trail> call() {
                ArrayList<Trail> trails = new ArrayList<>();
                StringBuilder request = new StringBuilder();
                request.append("https://www.trailforks.com/api/1/maptrails?output=encoded&api_key=").append(API_KEY).append("&filter=bbox%3A%3A").append(mapBounds.top).append(',')
                        .append(mapBounds.left).append(',').append(mapBounds.bottom).append(',').append(mapBounds.right);
                try {
                    URL url = new URL(request.toString());
                    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                    con.setDoInput(true);
                    con.setDoOutput(false);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    con.setRequestProperty("Host", "www.trailforks.com");
                    con.connect();
                    if(con.getResponseCode() == 200){
                        try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                            StringBuilder j = new StringBuilder();
                            String line;
                            while((line = reader.readLine()) != null)
                                j.append(line);
                            JSONObject resp = new JSONObject(j.toString());
                            JSONArray data = resp.getJSONArray("data");
                            for(int i = 0; i < data.length(); ++i)
                                trails.add(new Trail(data.getJSONObject(i)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ObserverHelper.sendTo(observer, new ObserverEventArgs(ObserverNotifications.REDRAW_NOTIFY));
                return trails;
            }
        });
    }
    public static boolean isOffroad(MapImage map){
        Bitmap bmp = map.img.getBitmap();
        int red = 0, green = 0, blue = 0;
        for(int x = 0; x < bmp.getWidth(); ++x){
            for(int y = 0; y < bmp.getHeight(); ++y){
                int color = bmp.getPixel(x, y);
                red += color & 0xFF0000;
                green += color & 0xFF00;
                blue += color & 0xFF;
            }
        }
        green /= bmp.getWidth() * bmp.getHeight();
        return green > 190;
    }
}
*/
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

