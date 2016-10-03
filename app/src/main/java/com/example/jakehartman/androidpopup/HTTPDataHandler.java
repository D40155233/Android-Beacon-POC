package com.example.jakehartman.androidpopup;

import android.util.Log;

import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.URL;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.MalformedURLException;

/**
 * Created by cfsuman on 31/05/2015.
 */
public class HTTPDataHandler {

    static String stream = null;

    public HTTPDataHandler(){
    }

    public String GetHTTPData(String urlString){
        try{
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty( "authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=");
            urlConnection.setRequestProperty( "dsi", "D40234627");

            // Check the connection status
            if(urlConnection.getResponseCode() == 200)
            {
                // if response code = 200 ok
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                // Read the BufferedInputStream
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
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
            else
            {
                InputStream in = new BufferedInputStream(urlConnection.getErrorStream());

                // Read the BufferedInputStream
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
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