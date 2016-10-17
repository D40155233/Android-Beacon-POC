package com.example.jakehartman.androidpopup;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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
import android.widget.Toast;

import okhttp3.*;
import java.io.IOException;
import okhttp3.MediaType;
import org.json.JSONTokener;


import java.util.Collection;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "DVGBeacon";
    public boolean showFeedbackOnResume = false;
    private static final String TAG = "BeaconReferenceAppF";

    private Context context = this;
    private BeaconReferenceApplication app;
    private BeaconManager beaconManager;
    private Dialog dialog;
    private SharedPreferences sharedPreferences;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    @TargetApi(23)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (BeaconReferenceApplication)getApplication();
        Button debugButton = (Button) findViewById(R.id.debugButton);
        debugButton.setVisibility(View.VISIBLE);

        debugButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, "Values Reset", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        displayLoginPopup();

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
    }


    /*///////////////////////////////////////////////
        Send Notifications to Phone
    *////////////////////////////////////////////////
    private void sendNotification(String text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("DeVry Education Group Beacon")
                        .setContentText(text);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_sms_failed_transparent_24dp);
            builder.setColor(getResources().getColor(R.color.devryGold));
        } else {
            builder.setSmallIcon(R.drawable.ic_sms_failed_black_24dp);
        }

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

    @Override
    public void onResume() {
        super.onResume();
        ((BeaconReferenceApplication) this.getApplicationContext()).setRangingActivity(this);
        if(app.isShowFeedbackOnResume() == true){
            displayFeedbackPopup();
        }
        Log.d(TAG, "RESUMED!!!!!");
    }

    @Override
    public void onPause() {
        super.onPause();
        ((BeaconReferenceApplication) this.getApplicationContext()).setRangingActivity(null);
        Log.d(TAG, "PAUSED!!!!!");
    }

    public void displayWelcomePopup() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.welcome_popup);
                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                dialog.show();

                Button dialogButtonOK = (Button) dialog.findViewById(R.id.dialogButtonOK);

                dialogButtonOK.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        app.setShowWelcomeOnResume(false);
                    }
                });
            }
        });
    }

    public void displayFeedbackPopup() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.feedback_popup);
                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                dialog.show();

                Button dialogButtonOK = (Button) dialog.findViewById(R.id.dialogButtonOK);

                dialogButtonOK.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick (View v){
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    public void displayLoginPopup() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.login_popup);
                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                dialog.show();

                final Button dialogButtonOK = (Button) dialog.findViewById(R.id.buttonSubmit);
                dialogButtonOK.setEnabled(false);

                EditText et = (EditText) dialog.findViewById(R.id.colleagueID);

                et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable arg0) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if(count == 0) {
                            dialogButtonOK.setEnabled(false);
                        }
                        if(count > 0) {
                            dialogButtonOK.setEnabled(true);
                        }
                    }
                });

                dialogButtonOK.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        app.bindBeacon();
                        dialog.dismiss();
                    }
                });
            }
        });
    }


}