package com.sev.activitylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.MODE_PRIVATE;

public class OAuth implements Subject, Runnable {
    public static final int CLIENT_ID = 34668;
    public static final String CLIENT_SECRET = "0a01bd0adee247b04f2605a7d78ffc5f11a9ed93";
    private String authCode;
    private String accessToken;
    private boolean authComplete;
    private LinkedList<Observer> observers;
    private String refreshToken;
    private long tokenExpiration; //seconds since epoch when token will expire
    public OAuth(){
        authComplete = false;
        observers = new LinkedList<Observer>();
    }
    public void setPreferences(SharedPreferences prefs){
        tokenExpiration = prefs.getLong("token_expiration", 0);
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
    }
    public void save(SharedPreferences prefs){
        if(authComplete){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("access_token", accessToken);
            editor.putString("refresh_token", refreshToken);
            editor.putLong("token_expiration", tokenExpiration);
            editor.commit();
        }
    }
    public boolean authenticateStep1() {
        long secs = System.currentTimeMillis() / 1000;
        if(secs < tokenExpiration - 3600){
            authComplete = true;
            return true;
        }else if(refreshToken != null){
            if(updateTokens()){
                authComplete = true;
                return true;
            }
        }
        sendNotify(new ObserverEventArgs(ObserverNotifications.URI_NOTIFY, new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.strava.com/oauth/authorize?client_id=" + Integer.toString(CLIENT_ID) + "&scope=activity:read_all&redirect_uri=http://127.0.0.1:8032&response_type=code"))));
        try (ServerSocket server = new ServerSocket(8032);
             Socket client = server.accept();){
            Log.d("AUTH 1", "Accepted Client");
            try(PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()))){
                String line;
                while((line = input.readLine()) != null){
                    if(line.contains("code=")){
                        Log.d("AUTH 1", "Got code!");
                        authCode = line.substring(line.indexOf("code=") + 5, line.indexOf('&', line.indexOf("code=")));
                        httpResponse(writer, true);
                        return true;
                    }
                }
                httpResponse(writer, false);
                return false;
            }
        }catch (IOException e){
            Log.e("AUTH 1", "IOE " + e.toString());
            return false;
        }
    }
    private void httpResponse(PrintWriter out, boolean success) {
        String response = "<html><h1>" + (success ? "Authentication Success" : "Authentication Failure") + "</h1><br><h5>You may now return to the app</h5></html>";
        StringBuilder build = new StringBuilder();
        build.append("Http/1.1 200 OK\r\nAccept-Ranges: bytes\r\nContent-Length: ");
        build.append(response.length());
        build.append("\r\nContent-Type: text/html\r\n\r\n");
        build.append(response);
        out.write(build.toString());
        out.flush();
        Log.d("AUTH 1", "Output");
    }
    public boolean authenticateStep2() {
        if(authComplete) return true;
        try {
            URL url = new URL("https://www.strava.com/oauth/token");
            StringBuilder query = new StringBuilder();
            query.append("{\"client_id\": \"").append(CLIENT_ID).append("\",").append("\"client_secret\": \"").append(CLIENT_SECRET).append("\",")
                    .append("\"code\": \"").append(authCode).append("\",").append("\"grant_type\": \"authorization_code\"}\r\n");
            String content = query.toString();
            HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(content.length()));
            connection.setRequestProperty("Host", "www.strava.com");
            connection.setRequestProperty("Accept", "application/json, */*");
            connection.setRequestProperty("User-Agent", "HTTPie/1.0.2");
            connection.setRequestProperty("Connection", "keep-alive");

            try(DataOutputStream out = new DataOutputStream(connection.getOutputStream())){
                out.writeBytes(content);
                out.flush();
                out.close();
                connection.connect();
            }
            Log.d("AUTH 2", "Sent request to strava");
            if(connection.getResponseCode() == 200){
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String msg;
                    StringBuilder builder = new StringBuilder();
                    while((msg = reader.readLine()) != null) {
                        builder.append(msg);
                    }
                    try {
                        JSONObject obj = new JSONObject(builder.toString());
                        accessToken = obj.getString("access_token");
                        tokenExpiration = obj.getLong("expires_at");
                        refreshToken = obj.getString("refresh_token");
                        authComplete = true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                Log.e("AUTH 2", "Response code is " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return authComplete;
    }

    public String getAuthCode(){ return authCode;}
    public String getAccessToken() {return accessToken;}
    public boolean isAuthComplete() {return authComplete;}

    @Override
    public void attach(Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        this.observers.remove(observer);
    }

    @Override
    public void run() {
        sendMsgToEventHandlers(authenticateStep1() && authenticateStep2());
    }
    private void sendMsgToEventHandlers(final boolean success){
        final OAuth param = this;
        new Handler(Looper.getMainLooper()).post(new Runnable(){ //Posts a runnable to be run on the main thread. Android only
            @Override
            public void run() {
                sendNotify(new ObserverEventArgs(ObserverNotifications.OAUTH_NOTIFY, param, success));
            }
        });
    }
    private void sendNotify(ObserverEventArgs e){
        for(Observer o : observers)
            o.notify(e);
    }
    private boolean updateTokens()
    {
        boolean success = false;
        try {
            URL url = new URL("https://www.strava.com/oauth/token");
            StringBuilder query = new StringBuilder();
            query.append("{\"client_id\": \"").append(CLIENT_ID).append("\",").append("\"client_secret\": \"").append(CLIENT_SECRET).append("\",")
                    .append("\"refresh_token\": \"").append(refreshToken).append("\",").append("\"grant_type\": \"refresh_token\"}\r\n");
            String content = query.toString();
            HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(content.length()));
            connection.setRequestProperty("Host", "www.strava.com");
            connection.setRequestProperty("Accept", "application/json, */*");
            connection.setRequestProperty("User-Agent", "HTTPie/1.0.2");
            connection.setRequestProperty("Connection", "keep-alive");

            try(DataOutputStream out = new DataOutputStream(connection.getOutputStream())){
                out.writeBytes(content);
                out.flush();
                out.close();
                connection.connect();
            }
            if(connection.getResponseCode() == 200){
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String msg;
                    StringBuilder jsonBuilder = new StringBuilder();
                    while((msg = reader.readLine()) != null) {
                        jsonBuilder.append(msg);
                    }
                    JSONObject json = new JSONObject(jsonBuilder.toString());
                    accessToken = json.getString("access_token");
                    tokenExpiration = json.getLong("expires_at");
                    refreshToken = json.getString("refresh_token");
                    authComplete = true;
                    success = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                Log.e("AUTH REFRESH", "Response code is " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

        } catch (MalformedURLException | ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

}
