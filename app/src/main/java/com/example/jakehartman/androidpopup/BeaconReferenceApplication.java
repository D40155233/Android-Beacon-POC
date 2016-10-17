package com.example.jakehartman.androidpopup;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BeaconReferenceApplication extends Application implements BootstrapNotifier, BeaconConsumer, RangeNotifier {
    private static final String TAG = "BeaconReferenceApp";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    public static Context context;
    private MainActivity rangingActivity = null;
    private boolean showFeedbackOnResume = false;
    private boolean showWelcomeOnResume = false;
    private boolean attendeeDataSent = false;
    private String android_id;
    private static final String PREFS_NAME = "DVGBeacon";
    private boolean haveDetectedBeaconsSinceBoot = false;
    private boolean welcomePopupShown = false;

    BeaconManager beaconManager;


    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Region region = new Region("backgroundRegion", Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"), Identifier.parse("0"), Identifier.parse("0"));
        regionBootstrap = new RegionBootstrap(this, region);

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        beaconManager.setBackgroundBetweenScanPeriod(1100l);
        //beaconManager.setForegroundBetweenScanPeriod(2000l);
        //beaconManager.bind(this);
    }

    @Override
    public void didEnterRegion(Region region) {
    }

    @Override
    public void didExitRegion(Region region) {
        //This will display the feedback popup if welcomePopup has been shown and they move outside the region of any beacons
        //Delete if logic changes
        if(welcomePopupShown && rangingActivity != null) {
            rangingActivity.displayFeedbackPopup();
        }
        else if(welcomePopupShown && rangingActivity == null) {
            showFeedbackOnResume = true;
            try {
                beaconManager.stopMonitoringBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Sending notification.");
            sendNotification("Please provide feedback for DeVry Education Group's IT Update Meeting!");
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG, "I have just switched from seeing/not seeing beacons: " + state);
        if(state == 1) {
            Log.d(TAG, "did enter region.");
            try {
                Log.d(TAG, "Starting Beacon Ranging");
                beaconManager.startRangingBeaconsInRegion(region);
            }
            catch (RemoteException e) {
                Log.d(TAG, "Can't start ranging");
            }
        }
        else {
            try {
                Log.d(TAG, "did exit region.");
                beaconManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.d(TAG, "Ranging...");
        if (beacons.size() > 0) {
            Beacon beacon = beacons.iterator().next();
            Log.d(TAG, "Beacon found!!!");
            Log.d(TAG, "Beacon is" + beacon.getDistance() + " meters away");
            if (!haveDetectedBeaconsSinceBoot && beacon.getDistance()<5) {
                Log.d(TAG, "auto launching MainActivity");

                // The very first time since boot that we detect an beacon, we launch the
                // MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Important:  make sure to add android:launchMode="singleInstance" in the manifest
                // to keep multiple copies of this activity from getting created if the user has
                // already manually launched the app.
                this.startActivity(intent);
                haveDetectedBeaconsSinceBoot = true;
            } else {
                if (rangingActivity != null && beacon.getDistance()<5) {
                    // If the Monitoring Activity is visible, we log info about the beacons we have
                    // seen on its display
                    if(welcomePopupShown == false) {
                        Log.d(TAG, "Showing popup!");
                        if(showWelcomeOnResume != true) {
                            rangingActivity.displayWelcomePopup();
                        }
                        showWelcomeOnResume = true;
                        welcomePopupShown = true;
                    }
                } else if (rangingActivity == null && beacon.getDistance()<5){
                    // If we have already seen beacons before, but the monitoring activity is not in
                    // the foreground, we send a notification to the user on subsequent detections.
                    if(welcomePopupShown == false) {
                        attendeeDataSent = true;
                        Log.d(TAG, "Sending notification.");
                        sendNotification("DeVry Education Group would like to welcome you!");
                    }
                }
                if(attendeeDataSent == false) {
                    sendAttendeeData(beacon.getId1().toUuid().toString());
                    attendeeDataSent = true;
                }
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(this);
    }

    public void setRangingActivity(MainActivity activity) {
        this.rangingActivity = activity;
    }

    private void sendNotification(String text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("DeVry Education Group")
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_dvg)
                        .setColor(getResources().getColor(R.color.devryGold));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public boolean isShowFeedbackOnResume() {
        Log.d(TAG, "isShowingFeedbackonResume status: " + showFeedbackOnResume);
        return showFeedbackOnResume;
    }

    public void setShowFeedbackOnResume(boolean bool) {
        showFeedbackOnResume = bool;
    }

    public boolean isShowWelcomeOnResume() {
        Log.d(TAG, "isShowingWelcomeOnResume status: " + showWelcomeOnResume);
        return showWelcomeOnResume;
    }

    public void setShowWelcomeOnResume(boolean bool) {
        showWelcomeOnResume = bool;
    }

    /*///////////////////////////////////////////////
        Process Get/Post request asynchronously
    *////////////////////////////////////////////////
    private class ProcessJSONAsync extends AsyncTask<Object, Void, Object> {
        protected Object doInBackground(Object... params){
            final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            Response response = null;
            Object finalResp = null;

            String urlString = (String) params[0];
            String callType  = (String) params[1];
            JSONObject headers  = (JSONObject) params[2];
            JSONObject body  = (JSONObject) params[3];

            if(callType == "GET") {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(urlString)
                        .addHeader("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=")
                        .addHeader("dsi", "D40234627")
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        Response resp = response;
                        String responseData = resp.body().string();
                        try {
                            JSONObject json = new JSONObject(responseData);
                            Log.d("STREAM", json.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //finalResp = resp.body().string();
                    }

                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "THERE WAS AN ERROR ON THE CALL! " + e);
                    }
                });
            }

            if(callType == "POST") {
                OkHttpClient client = new OkHttpClient();
                RequestBody rb = RequestBody.create(JSON, body.toString());
                Request request = new Request.Builder()
                        .url(urlString)
                        .addHeader("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=")
                        .addHeader("dsi", "D40234627")
                        .post(rb)
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        try {
                            Response resp = response;
                            String responseData = resp.body().string();
                            Log.d("STREAM", "YO" + resp.header("Content-Type"));
                            JSONObject json = new JSONObject(responseData);
                            Log.d("STREAM", json.toString());
                        } catch (JSONException e) {
                            Log.d("STREAM", "EXCEPTION");
                        }
                    }

                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            return finalResp;
        }
    }

    private void sendAttendeeData(String UUID) {
        JSONObject authHeaders = new JSONObject();
        String timeStamp = new java.util.Date().toString();
        try {
            authHeaders.put("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=");
            authHeaders.put("dsi", "D40234627");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject attendeeBody = new JSONObject();
        try {
            attendeeBody.put("deviceID", android_id);
            attendeeBody.put("beaconID", UUID);
            attendeeBody.put("os", "Android");
            attendeeBody.put("timestamp", timeStamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, android_id);
        new ProcessJSONAsync().execute("http://mblpocapp1.poc.devry.edu:9000/attendee", "POST", authHeaders, attendeeBody);
        new ProcessJSONAsync().execute("http://mblpocapp1.poc.devry.edu:9000/attendees", "GET", authHeaders, null);
    }

    public void bindBeacon() {
        beaconManager.bind(this);
    }
}