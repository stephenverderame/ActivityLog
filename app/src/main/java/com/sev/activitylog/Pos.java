package com.sev.activitylog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

    /**
     *
     * @param resolutionX dataPoints per degree longitude
     * @param resolutionY dataPoints per degree latitude
     * @param topLeft
     * @param btmRight
     * @return
     */
    public static Future<Pos[][]> getHeightMap(double resolutionX, double resolutionY, Pos topLeft, Pos btmRight){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            int width = (int)Math.round(Math.abs(btmRight.lon - topLeft.lon) * resolutionX);
            int height = (int)Math.round(Math.abs(topLeft.lat - btmRight.lat) * resolutionY);
            Pos[][] heightMap = new Pos[width][height];
            StringBuilder requestBuilder = new StringBuilder();
            requestBuilder.append("{\"locations\":[");
            for(int i = 0; i < width; ++i){
                for(int j = 0; j < height; ++j){
                    Pos p = new Pos(btmRight.lat + j / (double)resolutionY, topLeft.lon + i / (double)resolutionX);
                    requestBuilder.append("{\"latitude\": ").append(p.lat).append(",\"longitude\": ").append(p.lon).append("}");
                    if(i * j != (width - 1) * (height - 1)) requestBuilder.append(',');
                }
            }
            requestBuilder.append("]}");
            String request = requestBuilder.toString();
            try {
                URL url = new URL("https://open-elevation.com/api/v1/lookup");
                HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("Host", "open-elevation.com");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", String.valueOf(request.length()));
                con.setRequestProperty("Accept", "application/json");
                try(PrintWriter writer = new PrintWriter(new PrintStream(con.getOutputStream()))){
                    writer.print(request);
                }
                con.connect();
                if(con.getResponseCode() == 200){
                    StringBuilder resp = new StringBuilder();
                    try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                        String line;
                        while((line = reader.readLine()) != null)
                            resp.append(line);
                        JSONObject obj = new JSONObject(resp.toString());
                        JSONArray locations = obj.getJSONArray("results");
                        for(int i = 0; i < locations.length(); ++i){
                            JSONObject jsonPos = locations.getJSONObject(i);
                            heightMap[(int)Math.round(Math.abs(jsonPos.getDouble("longitude") - topLeft.lon) * resolutionX)]
                                    [(int)Math.round(Math.abs(jsonPos.getDouble("latitude") - btmRight.lat) * resolutionY)].elevation = jsonPos.getDouble("elevation");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return heightMap;
        });

    }
}
//Porting C++ code, probably a better Java way to do this
class Intptr{
    public int value;
}
