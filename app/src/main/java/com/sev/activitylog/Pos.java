package com.sev.activitylog;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class Pos {
    public double lat, lon;
    public double elevation;
    public Pos() {};
    public Pos(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    public Pos(double lat, double lon, double height){
        this(lat, lon);
        elevation = height;
    }
    public float[] toVec3f(){
        return new float[] {(float)lon, (float)elevation, (float)lat};
    }
    public static ArrayList<Pos> decodePolyline(String polyline, int precision) {
        ArrayList<Pos> points = new ArrayList<Pos>();
        Intptr i = new Intptr();
        i.value = 0;
        int lat = 0, lon = 0;
        while(i.value < polyline.length()){
            int latdiff = decodeHelper(polyline, i);
            int londiff = decodeHelper(polyline, i);
            lat += latdiff;
            lon += londiff;
            points.add(new Pos((double)lat / Math.pow(10, precision), (double)lon / Math.pow(10, precision)));
        }
        return points;

    }
    public static String encodePolyline(List<Pos> points, int precision){
        StringBuilder out = new StringBuilder();
        double lastLat = 0, lastLon = 0;
        for(Pos p : points){
            int val = (int)Math.round((p.lat - lastLat) * Math.pow(10, precision));
            ArrayList<Character> n = encodeHelper(val);
            for(char b : n)
                out.append(b);
            val = (int)Math.round((p.lon - lastLon) * Math.pow(10, precision));
            n = encodeHelper(val);
            for(char b : n)
                out.append(b);
            lastLat = p.lat;
            lastLon = p.lon;
        }
        return out.toString();
    }
    private static int decodeHelper(String polyline, Intptr index){
        int shift = 0, result = 0;
        byte bit = 0;
        do{
            bit = (byte)(polyline.charAt(index.value++) - 63);
            result |= (bit & 0x1f) << shift;
            shift += 5;
        } while(bit >= 0x20);
        return ((result & 1) == 1) ? ~(result >> 1) : (result >> 1);
    }
    private static ArrayList<Character> encodeHelper(int num){
        ArrayList<Character> buffer = new ArrayList<>();
        int val = num < 0 ? ~(num << 1) : (num << 1);
        int shift = 0;
        boolean moreChunks = false;
        do{
            int chunk = (val >> shift);
            shift += 5;
            moreChunks = chunk > 0x20;
            chunk &= 0b11111;
            if(moreChunks){
                chunk |= 0x20;
            }
            chunk += 63;
            buffer.add((char)chunk);
        } while(moreChunks);
        return buffer;
    }

    /**
     *
     * @param resolutionX dataPoints per degree longitude
     * @param resolutionY dataPoints per degree latitude
     * @param topLeft
     * @param btmRight
     * @return Future to 2D array of Positions in row major order ([y][x])
     */
    public static Future<Pos[][]> getHeightMap(double resolutionX, double resolutionY, Pos topLeft, Pos btmRight){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            int width = (int)Math.round(Math.abs(btmRight.lon - topLeft.lon) * resolutionX);
            int height = (int)Math.round(Math.abs(topLeft.lat - btmRight.lat) * resolutionY);
            Pos[][] heightMap = new Pos[height][width];
            ArrayList<Pos> positions = new ArrayList<Pos>();
            for(int i = 0; i < height; ++i){
                for(int j = 0; j < width; ++j){
                    Pos p = new Pos(btmRight.lat + i / (double)resolutionY, topLeft.lon + j / (double)resolutionX);
                    heightMap[i][j] = p;
                    positions.add(p);
                }
            }
            Log.d("Fetch Height", "Built request!");
            int start = 0, end = 0;
            do {
                start = end;
                end = Math.min(end + 100, positions.size());
                List<Pos> req = positions.subList(start, end); //max 100 queries per request
                try {
                    URL url = new URL("https://api.opentopodata.org/v1/srtm30m?locations=" + Pos.encodePolyline(req, 5));
                    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                    con.setDoOutput(false);
                    con.setDoInput(true);
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Host", "api.opentopodata.org");
                    con.setRequestProperty("Accept", "application/json");
                    con.connect();
                    if (con.getResponseCode() == 200) {
                        Log.d("Height Fetch", "Got response!");
                        StringBuilder resp = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null)
                                resp.append(line);
                            JSONObject obj = new JSONObject(resp.toString());
                            if(obj.getString("status").equals("OK")) {
                                JSONArray locations = obj.getJSONArray("results");
                                for (int i = 0; i < locations.length(); ++i) {
                                    JSONObject jsonPos = locations.getJSONObject(i);
                                    JSONObject location = jsonPos.getJSONObject("location");
                                    heightMap[(int) Math.round(Math.abs(location.getDouble("lat") - btmRight.lat) * resolutionY)]
                                            [(int) Math.round(Math.abs(location.getDouble("lng") - topLeft.lon) * resolutionX)].elevation = jsonPos.getDouble("elevation") * RideOverview.METERS_MILES_CONVERSION / 10; //puts elevation onto a similar scale as lat + lon
                                }
                            }else{
                                Log.e("Height Fetch", "Error getting data " + obj.getString("status"));
                                Log.e("Height Fetch", obj.getString("error"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(1010); //max 1 request per second
                    } else
                        Log.e("Height Fetch", con.getResponseCode() + " " + con.getResponseMessage());
                } catch (MalformedURLException e) {
                    Log.e("Height Fetch", e.toString());
                } catch (IOException e) {
                    Log.e("Height Fetch", e.toString());
                }
            } while(end < positions.size());
            return heightMap;
        });

    }
}
//Porting C++ code, probably a better Java way to do this
class Intptr{
    public int value;
}
