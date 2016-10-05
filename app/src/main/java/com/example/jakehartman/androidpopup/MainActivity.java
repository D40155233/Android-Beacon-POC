package com.example.jakehartman.androidpopup;

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
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.lang.Object;
import android.os.*;

import java.util.Collection;

public class MainActivity extends Activity implements BeaconConsumer {

    private Context context = this;
    //private Activity activity;
    private Button showWelcomePopup;
    private Button showFeedbackPopup;
    private BeaconManager beaconManager;
    private Handler mHandler =new Handler();
    //private Dialog feedback = new Dialog(context);
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    @TargetApi(23)
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconManager = BeaconManager.getInstanceForApplication(context);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

        showWelcomePopup = (Button) findViewById(R.id.buttonShowWelcomeDialog);
        showFeedbackPopup = (Button) findViewById(R.id.buttonShowFeedbackDialog);

        //feedback.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //feedback.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;

        String urlString = "http://mbldevapp1.dev.devry.edu:8080/DVG-CustomerEngagement-Services/api/customerengagement/";
//        String callType  = "GET";
        JSONObject authHeaders = new JSONObject();
        try {
            authHeaders.put("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=");
            authHeaders.put("dsi", "D40234627");
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
        new ProcessJSON().execute("http://mbldevapp1.dev.devry.edu:8080/DVG-CustomerEngagement-Services/api/customerengagement/", "GET", authHeaders, null);
        new ProcessJSON().execute("http://mbldevapp1.dev.devry.edu:8080/DeVry-Mobile-Services/api/servicenow?sysparm_display_value=true", "POST", authHeaders, body);



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

    //Permissions used to perform functionality in the app.

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
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

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
                boolean triggered = false;

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
                                final Dialog dialog = new Dialog(context);
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                dialog.setContentView(R.layout.feedback_popup);
                                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;

                            }
                        });
                    }
                }
            });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("6fbbef7c-f92c-471e-8d5c-470e9b367fdb"), Identifier.parse("0"), Identifier.parse("0")));
            beaconManager.startMonitoringBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("6fbbef7c-f92c-471e-8d5c-470e9b367fdb"), Identifier.parse("0"), Identifier.parse("0")));
            //beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("1b09c0cb-63cf-4b31-af1e-646277bd8b49"), Identifier.parse("25"), Identifier.parse("5")));
            //beaconManager.startMonitoringBeaconsInRegion(new Region("myRangingUniqueId", Identifier.parse("1b09c0cb-63cf-4b31-af1e-646277bd8b49"), Identifier.parse("25"), Identifier.parse("5")));
            //beaconManager.updateScanPeriods();
        } catch (RemoteException e) {   }
    }

    private class ProcessJSON extends AsyncTask<Object, Void, String>{
        protected String doInBackground(Object... params){
            String stream = null;
            String urlString = (String) params[0];
            String callType  = (String) params[1];
            Log.i("STREAM", callType);
            JSONObject headers  = (JSONObject) params[2];
            JSONObject body  = (JSONObject) params[3];

            HTTPDataHandler hh = new HTTPDataHandler();
            stream = hh.GetHTTPData(urlString, callType, headers, body);

            // Return the data from specified url
            return stream;
        }

        protected void onPostExecute(String stream){
            //TextView tv = (TextView) findViewById(R.id.tv);
            //tv.setText(stream);

            /*
                Important in JSON DATA
                -------------------------
                * Square bracket ([) represents a JSON array
                * Curly bracket ({) represents a JSON object
                * JSON object contains key/value pairs
                * Each key is a String and value may be different data types
             */

            //..........Process JSON DATA................
            if(stream !=null){
                try{
                    // Get the full HTTP Data as JSONObject
                    JSONObject reader= new JSONObject(stream);

                    // Get the JSONObject "coord"...........................
                    JSONObject coord = reader.getJSONObject("coord");
                    // Get the value of key "lon" under JSONObject "coord"
                    String lon = coord.getString("lon");
                    // Get the value of key "lat" under JSONObject "coord"
                    String lat = coord.getString("lat");

//                    tv.setText("We are processing the JSON data....\n\n");
//                    tv.setText(tv.getText()+ "\tcoord...\n");
//                    tv.setText(tv.getText()+ "\t\tlon..."+ lon + "\n");
//                    tv.setText(tv.getText()+ "\t\tlat..."+ lat + "\n\n");

                    // Get the JSONObject "sys".........................
                    JSONObject sys = reader.getJSONObject("sys");
                    // Get the value of key "message" under JSONObject "sys"
                    String message = sys.getString("message");
                    // Get the value of key "country" under JSONObject "sys"
                    String country = sys.getString("country");
                    // Get the value of key "sunrise" under JSONObject "sys"
                    String sunrise = sys.getString("sunrise");
                    // Get the value of key "sunset" under JSONObject "sys"
                    String sunset = sys.getString("sunset");

//                    tv.setText(tv.getText()+ "\tsys...\n");
//                    tv.setText(tv.getText()+ "\t\tmessage..."+ message + "\n");
//                    tv.setText(tv.getText()+ "\t\tcountry..."+ country + "\n");
//                    tv.setText(tv.getText()+ "\t\tsunrise..."+ sunrise + "\n");
//                    tv.setText(tv.getText()+ "\t\tsunset..."+ sunset + "\n\n");

                    // Get the JSONArray weather
                    JSONArray weatherArray = reader.getJSONArray("weather");
                    // Get the weather array first JSONObject
                    JSONObject weather_object_0 = weatherArray.getJSONObject(0);
                    String weather_0_id = weather_object_0.getString("id");
                    String weather_0_main = weather_object_0.getString("main");
                    String weather_0_description = weather_object_0.getString("description");
                    String weather_0_icon = weather_object_0.getString("icon");

//                    tv.setText(tv.getText()+ "\tweather array...\n");
//                    tv.setText(tv.getText()+ "\t\tindex 0...\n");
//                    tv.setText(tv.getText()+ "\t\t\tid..."+ weather_0_id + "\n");
//                    tv.setText(tv.getText()+ "\t\t\tmain..."+ weather_0_main + "\n");
//                    tv.setText(tv.getText()+ "\t\t\tdescription..."+ weather_0_description + "\n");
//                    tv.setText(tv.getText()+ "\t\t\ticon..."+ weather_0_icon + "\n\n");

                    // process other data as this way..............

                }catch(JSONException e){
                    e.printStackTrace();
                }

            } // if statement end
        } // onPostExecute() end
    } // ProcessJSON class end


}