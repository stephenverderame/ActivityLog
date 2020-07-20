package com.sev.activitylog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Context.MODE_PRIVATE;

/**
 * Handles strava OAuth authentication
 * Should be started in its own thread or other async object
 * Sends the OAUTH_NOTIFY event when authentication is complete (either fail or success)
 */
public class OAuth implements Subject, Runnable {
    public static final int CLIENT_ID = 34668;
    public static final String CLIENT_SECRET = "0a01bd0adee247b04f2605a7d78ffc5f11a9ed93";
    private String authCode;
    private AtomicBoolean authenticating, authComplete;
    private LinkedList<Observer> observers;
    private AuthToken token;
    public OAuth(AuthToken token){
        authComplete = new AtomicBoolean(false);
        authenticating = new AtomicBoolean(false);
        observers = new LinkedList<Observer>();
        this.token = token;
    }

    /**
     * Step 1: Getting the auth code from the user. If no token is saved, prompts the user with the OAuth webpage
     * When the user submits the page, the page redirects the HTTP request to the local host HTTP server
     * This server collects the auth code from the user to be used to get an access token in step 2.
     * @return success boolean
     */
    public synchronized boolean authenticateStep1() {
        long secs = System.currentTimeMillis() / 1000;
        if(secs < token.getExpiration() - 3600){
            authComplete.set(true);
            return true;
        }else if(token.getRefreshToken() != null){
            if(updateTokens()){
                authComplete.set(true);
                return true;
            }
        }
        ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.URI_NOTIFY,
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.strava.com/oauth/authorize?client_id=" + Integer.toString(CLIENT_ID) + "&scope=activity:read_all&redirect_uri=http://127.0.0.1:8032&response_type=code"))));
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

    /**
     * Returns an HTML page from the local HTTP server to the user after an auth code has been successfully (or unsuccessfully) received
     * @param out outputStream of socket
     * @param success
     */
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

    /**
     * Step 2: uses auth code from step 1 to get an access token from strava.
     * Sends a JSON HTTPS request to strava servers requesting an authorization token
     * Token is encapsulated in an AuthToken
     * @return success
     * @see AuthToken
     */
    public synchronized boolean authenticateStep2() {
        if(authComplete.get()) return true;
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
                        token.setAccessToken(obj.getString("access_token"));
                        token.setExpiration(obj.getLong("expires_at"));
                        token.setRefreshToken(obj.getString("refresh_token"));
                        authComplete.set(true);
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
        return authComplete.get();
    }

    public synchronized String getAuthCode(){ return authCode;}
    public synchronized AuthToken getAuthToken() {return token;}
    public boolean isAuthComplete() {return authComplete.get();}

    @Override
    public void attach(Observer observer) {
        synchronized (observers) {
            this.observers.add(observer);
        }
    }

    @Override
    public void detach(Observer observer) {
        synchronized (observers) {
            this.observers.remove(observer);
        }
    }

    @Override
    public void run() {
        if(!authenticating.get() && !authComplete.get()) {
            authenticating.set(true);
            boolean success = authenticateStep1() && authenticateStep2();
            authenticating.set(false);
            token.save();
            ObserverHelper.sendToObservers(observers, new ObserverEventArgs(ObserverNotifications.OAUTH_NOTIFY, OAuth.this, success));
        }
    }
    public boolean isAuthenticating() {return authenticating.get();}
    private synchronized boolean updateTokens()
    {
        boolean success = false;
        try {
            URL url = new URL("https://www.strava.com/oauth/token");
            StringBuilder query = new StringBuilder();
            query.append("{\"client_id\": \"").append(CLIENT_ID).append("\",").append("\"client_secret\": \"").append(CLIENT_SECRET).append("\",")
                    .append("\"refresh_token\": \"").append(token.getRefreshToken()).append("\",").append("\"grant_type\": \"refresh_token\"}\r\n");
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
                    token.setAccessToken(json.getString("access_token"));
                    token.setExpiration(json.getLong("expires_at"));
                    token.setRefreshToken(json.getString("refresh_token"));
                    authComplete.set(true);
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

/*
Parcelable interface is desgined to send objects from one activity to another
 */
class AuthToken implements Parcelable{
    public static final String ACCESS_TOKEN_ID = "access_token", REFRESH_TOKEN_ID = "refresh_token", EXPIRATION_ID = "token_expiration";
    public static final String PREFERENCES_ID = "auth_token";
    private String accessToken, refreshToken;
    private long expiration; //seconds since epoch when token will expire
    private SharedPreferences prefs;
    public AuthToken(SharedPreferences prefs){
        expiration = 0;
        this.prefs = prefs;
        accessToken = prefs.getString(ACCESS_TOKEN_ID, null);
        refreshToken = prefs.getString(REFRESH_TOKEN_ID, null);
        expiration = prefs.getLong(EXPIRATION_ID, 0);
    }

    public static void clear(SharedPreferences prefs){
        if(prefs != null) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_TOKEN_ID, null);
            edit.putString(REFRESH_TOKEN_ID, null);
            edit.putLong(EXPIRATION_ID, 0);
            edit.commit();
        }
    }

    public synchronized String getAccessToken() {return accessToken;}
    public synchronized String getRefreshToken() {return refreshToken;}
    public synchronized long getExpiration() {return expiration;}
    public synchronized void setAccessToken(String tk) {accessToken = tk;}
    public synchronized void setRefreshToken(String tk) {refreshToken = tk;}
    public synchronized void setExpiration(long exp) {expiration = exp;}
    public synchronized boolean isValid() {return System.currentTimeMillis() / 1000 < expiration;}
    public synchronized void save(){
        if(prefs != null) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_TOKEN_ID, accessToken);
            edit.putString(REFRESH_TOKEN_ID, refreshToken);
            edit.putLong(EXPIRATION_ID, expiration);
            edit.commit();
        }
    }


    private AuthToken(Parcel in) {
        accessToken = in.readString();
        refreshToken = in.readString();
        expiration = in.readLong();
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(accessToken);
        parcel.writeString(refreshToken);
        parcel.writeLong(expiration);
    }
    public static final Creator<AuthToken> CREATOR = new Creator<AuthToken>() {
        @Override
        public AuthToken createFromParcel(Parcel in) {
            return new AuthToken(in);
        }

        @Override
        public AuthToken[] newArray(int size) {
            return new AuthToken[size];
        }
    };
}
