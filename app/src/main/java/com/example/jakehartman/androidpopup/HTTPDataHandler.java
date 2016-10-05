package com.example.jakehartman.androidpopup;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.URL;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.io.DataOutputStream;

/**
 * Created by cfsuman on 31/05/2015.
 */
public class HTTPDataHandler {

    static String stream = null;

    public HTTPDataHandler(){
    }

    public String GetHTTPData(String urlString, String callType, JSONObject headers, JSONObject body){
        try{
            URL url = new URL(urlString);

            if(callType == "GET") {
                Log.i("STREAM","It's a GET call dawg!");
                InputStream getIS;
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //Iterate over the keys and set the request properties
                Iterator<?> keys = headers.keys();

                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    try {
                        String value = headers.getString(key);
                        urlConnection.setRequestProperty(key, value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                Log.i("STREAM", "R" + urlConnection.getResponseCode());
                if(urlConnection.getResponseCode() == 200) {
                    getIS = new BufferedInputStream(urlConnection.getInputStream());
                }
                else {
                    getIS = new BufferedInputStream(urlConnection.getErrorStream());
                }

                // Read the BufferedInputStream
                BufferedReader r = new BufferedReader(new InputStreamReader(getIS));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
                stream = sb.toString();
                Log.i("STREAM", stream);
                // End reading...............


                // Disconnect the HttpURLConnection
                urlConnection.disconnect();
            }

            if(callType == "POST") {
                Log.i("STREAM","It's a post call dawg!");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //Iterate over the keys and set the request properties
                Iterator<?> keys = headers.keys();

                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    try {
                        String value = headers.getString(key);
                        urlConnection.setRequestProperty(key, value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
                dStream.writeBytes(body.toString()); //Writes out the string to the underlying output stream as a sequence of bytes
                dStream.flush(); // Flushes the data output stream.
                dStream.close(); // Closing the output stream.
                Log.i("STREAM", "Closed dStream");
                InputStream postIS = new BufferedInputStream(urlConnection.getInputStream());
//                if(urlConnection.getResponseCode() != 200) {
//                    postIS = new BufferedInputStream(urlConnection.getErrorStream());
//                }
                Log.i("STREAM", "Reading Buffered Input Stream");

                // Read the BufferedInputStream
                BufferedReader r = new BufferedReader(new InputStreamReader(postIS));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
                stream = sb.toString();
                Log.i("STREAM", stream);
                // End reading...............

                // Disconnect the HttpURLConnection
                urlConnection.disconnect();
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch(IOException e){
            Log.i("STREAM", "CAUGHT");
            e.printStackTrace();
        }finally {

        }
        // Return the data from specified url
        return stream;
    }
}