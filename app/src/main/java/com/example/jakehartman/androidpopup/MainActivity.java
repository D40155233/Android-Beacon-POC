package com.example.jakehartman.androidpopup;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.lang.Object;
import android.app.TaskStackBuilder;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.content.Intent;
import okhttp3.*;
import java.io.IOException;
import okhttp3.MediaType;
import org.json.JSONTokener;


import java.util.Collection;

public class MainActivity extends Activity implements BeaconConsumer {

    public static final String PREFS_NAME = "DVGBeacon";

    private Context context = this;
    private Button showWelcomePopup;
    private Button showFeedbackPopup;
    private BeaconManager beaconManager;
    private Boolean welcomeTriggered;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    @TargetApi(23)
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        welcomeTriggered = settings.getBoolean("welcomeTriggered", false);
        setContentView(R.layout.activity_main);
        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

        showWelcomePopup = (Button) findViewById(R.id.buttonShowWelcomeDialog);
        showFeedbackPopup = (Button) findViewById(R.id.buttonShowFeedbackDialog);

        JSONObject authHeaders = new JSONObject();
        try {
            authHeaders.put("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=");
            authHeaders.put("dsi", "D40234627");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String timeStamp = new java.util.Date().toString();
        Log.d("STREAM", timeStamp);
        JSONObject attendeeBody = new JSONObject();
        try {
            attendeeBody.put("deviceID", "D01317819");
            attendeeBody.put("beaconID", "Self-service");
            attendeeBody.put("os", "Android");
            attendeeBody.put("timestamp", timeStamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JSONObject body = new JSONObject();
        try {
            body.put("caller_id", "D01317819");
            body.put("contact_type", "Self-service");
            body.put("short_description", "Short Test");
            body.put("description", "Long Test");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //JSONObject customerEngagementData = (JSONObject) ProcessJSON("http://mbldevapp1.dev.devry.edu:8080/DVG-CustomerEngagement-Services/api/customerengagement/", "GET", authHeaders, null);
        //JSONArray attendees = (JSONArray) ProcessJSON("http://mblpocapp1.poc.devry.edu:9000/attendees", "GET", authHeaders, null);
        //new ProcessJSONAsync().execute("http://mblpocapp1.poc.devry.edu:9000/attendee", "POST", authHeaders, attendeeBody);



        // add button listener
        showWelcomePopup.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                // custom dialog
                final Dialog dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.welcome_popup);
                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;


                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        showFeedbackPopup.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                // custom dialog
                final Dialog dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.feedback_popup);
                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;

                // set the custom dialog components - text, image and button
//                TextView text = (TextView) dialog.findViewById(R.id.text);
//                text.setText("Android custom dialog example!");
//                ImageView image = (ImageView) dialog.findViewById(R.id.image);
//                image.setImageResource(R.drawable.ic_launcher);

                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    /*///////////////////////////////////////////////
        Beacon Service Callbacks
    *////////////////////////////////////////////////
    @Override
    public void onBeaconServiceConnect() {
        final TextView tv = (TextView) findViewById(R.id.textView4);
        final TextView tv2 = (TextView) findViewById(R.id.textView5);
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv2.setText("Entered Monitored Region");
                        final Dialog dialog = new Dialog(context);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.feedback_popup);
                        dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                        dialog.show();
                    }
                });

            }

            @Override
            public void didExitRegion(Region region) {
                Log.i("not in range", "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i("switched", "I have just switched from seeing/not seeing beacons: " + state);
            }
        });


            beaconManager.addRangeNotifier(new RangeNotifier() {

                @Override
                public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                    if (beacons.size() > 0) {
                        final Beacon firstBeacon = beacons.iterator().next();
                        Log.d("BEACON", firstBeacon.getRssi() + "The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");
                        Log.d("BEACON", "INFO: " + "Power:" + firstBeacon.getTxPower() + "" + firstBeacon.getBeaconTypeCode());
                        Log.d("BEACON", "YO");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("Your distance is " + firstBeacon.getDistance());
                                if (firstBeacon.getDistance() < 5 && welcomeTriggered == false) {
                                    welcomeTriggered = true;
                                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                    SharedPreferences.Editor editor = settings.edit();
                                    editor.putBoolean("welcomeTriggered", true );
                                    editor.commit();
                                    sendNotification("DVG wants to welcome you!");
                                    final Dialog dialog = new Dialog(context);
                                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                    dialog.setContentView(R.layout.feedback_popup);
                                    dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                                    dialog.show();
                                }
                            }
                        });
                    }
                }
            });

        try {
            //beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("6FBBEF7C-F92C-471E-8D5C-470E9B367FDB"), Identifier.parse("0"), Identifier.parse("0")));
            //beaconManager.startMonitoringBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("6FBBEF7C-F92C-471E-8D5C-470E9B367FDB"), Identifier.parse("0"), Identifier.parse("0")));
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("48caffe0-c786-4ab9-85f3-6585ace3baee"), Identifier.parse("0"), Identifier.parse("0")));
            beaconManager.startMonitoringBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("48caffe0-c786-4ab9-85f3-6585ace3baee"), Identifier.parse("0"), Identifier.parse("0")));
            //beaconManager.updateScanPeriods();
        } catch (RemoteException e) {   }
    }

    /*///////////////////////////////////////////////
        Send Notifications to Phone
    *////////////////////////////////////////////////
    private void sendNotification(String text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("DeVry Education Group Beacon")
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_launcher);

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

    /*///////////////////////////////////////////////
        Process Get/Post request synchronized
    *////////////////////////////////////////////////
    private Object ProcessJSON(Object... params) {
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

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
            Response response = null;
            try {
                Log.i("Stream", "Trying get...");
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Log.i("STREAM", response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
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

            Response response = null;
            try {
                Log.i("Stream", "Trying post...");
                response = client.newCall(request).execute();
            } catch (IOException e) {
                Log.i("Stream", "EXCEPTION");
                e.printStackTrace();
            }
            try {
                Log.i("STREAM", response.body().string());
            } catch (IOException e) {
                Log.i("Stream", "EXCEPTION!!!");
                e.printStackTrace();
            }
        }
        return "hi";
    }

    /*///////////////////////////////////////////////
        Process Get/Post request asynchronously
    *////////////////////////////////////////////////
    private class ProcessJSONAsync extends AsyncTask<Object, Void, Object>{
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
                            //finalResp = resp.body().string();
                    }

                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
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
                            Log.d("STREAM", response.body().string());
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


    /*///////////////////////////////////////////////
     Permissions used to perform functionality in the app.
    *////////////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("message", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


}