package com.dvg.jakehartman.beaconpoc;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.vdurmont.emoji.EmojiParser;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private MainActivity mainActivity = null;
    private String userID = "Unknown";
    private JSONArray beaconArray;
    private List<Beacon> beaconList;
    private boolean attendeeDataSent = false;
    private boolean userWithinRange = false;
    private boolean meetingStarted = false;
    private boolean retrieveBeaconListError = false;
    private boolean welcomePopupShownbool = false;
    private String userInitialUUID = "Unknown";
    private String android_id;
    private ArrayList<Region> regions;
    private static final String PREFS_NAME = "DVGBeacon";
    private boolean haveDetectedBeaconsSinceBoot = false;
    private boolean welcomePopupShown = false;
    private Intent firebaseServiceIntent;
    private MyFirebaseMessagingService mfbs;
    private NotificationManager notificationManager;

    BeaconManager beaconManager;


    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        regions = new ArrayList<Region>();
        //BeaconManager init
        initializeBeaconManager();
        //Firebase init
        firebaseServiceIntent = new Intent(this, MyFirebaseMessagingService.class);
        retrieveBeaconList();

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        Log.d(TAG, "Starting Firebase Services");
        startService(firebaseServiceIntent);
        Log.d(TAG, "Completed Starting Firebase Services");
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "Entered a region!!!" + region.getId1());
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        }
        catch (RemoteException e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Can't start ranging");
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG, "Exited a region!!!" + region.getId1());
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG,"I have just switched from seeing/not seeing beacons: " + state);
        if(state == 1) {
            didEnterRegion(region);
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.d(TAG, "Ranging for " + region.getId1());
        if (beacons.size() > 0) {
            Beacon beacon = beacons.iterator().next();
            Log.d(TAG, "Beacon ID is " + beacon.getId1());
            Log.d(TAG, "Beacon is" + beacon.getDistance() + " meters away");
                if (mainActivity != null && beacon.getDistance()<7) {

                    // If the Monitoring Activity is visible, we log info about the beacons we have
                    // seen on its display
                        userWithinRange = true;
                        userInitialUUID = beacon.getId1().toUuid().toString();
                        Log.d(TAG, "Showing welcome popup because in foreground!");
                        mainActivity.displayWelcomePopup();
                        mainActivity.setBeaconFoundFlag(userInitialUUID);
                } else if (mainActivity == null && beacon.getDistance()<7){
                        Log.d(TAG, "sending Welcome Notification because in background");
                        userWithinRange = true;
                        attendeeDataSent = true;
                        if(welcomePopupShownbool == false) {
                            sendWelcomeNotification(userInitialUUID);
                        }
                }
                if (attendeeDataSent == false) {
                    userInitialUUID = beacon.getId1().toUuid().toString();
                }
        }
        if(userWithinRange == true) {
            Log.d(TAG, "Stopping Monitoring and Ranging");
            beaconManager.unbind(this);
        }
    }

    public void removeAllBeaconNotifiers() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.removeAllMonitorNotifiers();
    }



    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(this);
    }

    public void setMainActivity(MainActivity activity) {
        this.mainActivity = activity;
    }

//    public void setShowWelcomeOnResume(boolean bool) {
//        showWelcomeOnResume = bool;
//    }

    private void retrieveBeaconList() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://ec2-52-44-53-47.compute-1.amazonaws.com:8080/DVG-CustomerEngagement-Services/api/customerengagement/beacon_location")
                .addHeader("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=")
                .addHeader("dsi", "D40234627")
                .build();
        client.newCall(request).enqueue(new Callback() {
            //If Service Call Bad:

            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Get Beacons Failed!");
            }

            //If Service Call Good:
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Response r = response;
                String responseString = r.body().string();
                Log.d(TAG, "RESPONSE CODE! " + r.code());
                Log.d(TAG, "RESPONSE! " + responseString);
                if(r.code() == 200) {
                    try {
                        beaconArray = new JSONArray(responseString);
                        regions = new ArrayList<Region>();
                        Log.d(TAG, beaconArray.toString());
                        for (int i = 0; i < beaconArray.length(); i++) {
                            JSONObject temp = beaconArray.getJSONObject(i);
                            regions.add(new Region(temp.getString("room_name"), Identifier.parse(temp.getString("beacon_id")), null, null));
                        }
                        //bootstrapRegions(regions);
                    } catch (JSONException e) {
                        retrieveBeaconListError = true;
                        e.printStackTrace();
                    }
                }
                else {
                    retrieveBeaconListError = true;
                }
            }
        });
    }

    public void sendAttendeeData() {
        OkHttpClient client = new OkHttpClient();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject attendeeBody = new JSONObject();
        try {
            attendeeBody.put("attendeeID", userID);
            attendeeBody.put("beaconID", userInitialUUID);
            attendeeBody.put("eventID", "1");
            attendeeBody.put("eventName", "IT All Hands Meeting");
            attendeeBody.put("timestamp", timeStamp);
            attendeeBody.put("os", "Android");
            attendeeBody.put("deviceID", android_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, attendeeBody.toString());
        RequestBody rb = RequestBody.create(JSON, attendeeBody.toString());
        Log.d(TAG, android_id);
        Request request = new Request.Builder()
                .url("http://ec2-52-44-53-47.compute-1.amazonaws.com:8080/DVG-CustomerEngagement-Services/api/customerengagement/attendance")
                .addHeader("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=")
                .addHeader("dsi", "D40234627")
                .post(rb)
                .build();
        client.newCall(request).enqueue(new Callback() {
            //If Service Call Bad:

            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Attendee Data Sent Failed!");
                //Toast.makeText(getApplicationContext(), "Unable to send attendee data", Toast.LENGTH_LONG).show();

            }
            //If Service Call Good:

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Response r = response;
                Log.d(TAG, "RESPONSE CODE! " + r.code());
                Log.d(TAG, "RESPONSE! " + r.body().string());
            }
        });
    }

    public void sendFeedbackData(String ID, int rating, String comment) {
        OkHttpClient client = new OkHttpClient();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String parsedComment = EmojiParser.removeAllEmojis(comment);
        JSONObject feedbackBody = new JSONObject();
        try {
            feedbackBody.put("attendee_id", userID);
            feedbackBody.put("event_id", "1");
            feedbackBody.put("question_id", ID);
            feedbackBody.put("rating_amount", rating);
            feedbackBody.put("rating_comment", parsedComment);
            feedbackBody.put("ratingtimestamp", timeStamp);
            feedbackBody.put("beaconID", userInitialUUID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, feedbackBody.toString());
        RequestBody rb = RequestBody.create(JSON, feedbackBody.toString());
        Request request = new Request.Builder()
                .url("http://ec2-52-44-53-47.compute-1.amazonaws.com:8080/DVG-CustomerEngagement-Services/api/customerengagement/ratings_by_question_and_user")
                .addHeader("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=")
                .addHeader("dsi", "D40234627")
                .post(rb)
                .build();
        client.newCall(request).enqueue(new Callback() {
            //If Service Call Bad:

            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Ratings Call Failed!");
                //Toast.makeText(getApplicationContext(), "Feedback not submitted successfully.", Toast.LENGTH_LONG).show();
            }
            //If Service Call Good:

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Response r = response;
                Log.d(TAG, "RESPONSE CODE! " + r.code());
                Log.d(TAG, "RESPONSE! " + r.body().string());
                //Toast.makeText(getApplicationContext(), "Feedback submitted successfully.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void bindBeacon(String ID) {
        Log.d(TAG, "bindBeacon has been hit!");
        userID = ID;
        FirebaseMessaging.getInstance().subscribeToTopic("reminders");
        FirebaseMessaging.getInstance().subscribeToTopic("feedback");
    }

    public void setWelcomePopupShown(boolean tempbool) {
        welcomePopupShownbool = tempbool;
    }

    public void bootstrapRegions(ArrayList<Region> r) throws RemoteException {
        regionBootstrap = new RegionBootstrap(this, r);
        Log.d(TAG, "bootstrappedRegions");
    }

    public void feedbackInfoReceived(Map<String, String> map) {
        Log.d(TAG, "FeedbackInfoReceived!");
        Log.d(TAG, "HASHMAP! " + map);
        //Log.d(TAG, "QUESTION TEXT!" + map.get("questionText"));
        //Main Activity is up, display pop-up
        if (mainActivity != null) {
            mainActivity.displayFeedbackPopup(map.get("questionText"), map.get("questionNumber"));
        }
        //Main Activity is not up, send notification
        else {
            sendFeedbackNotification(map);

        }
    }

    public void reminderInfoReceived(Map<String, String> map) {
        Log.d(TAG, "ReminderInfoReceived!");
        Log.d(TAG, "HASHMAP! " + map);
        sendReminderNotification();
        startBeaconing();
        if(mainActivity != null) {
            mainActivity.setMeetingStartedFlag();
        }
        FirebaseMessaging.getInstance().unsubscribeFromTopic("reminders");
//            else {
//                mainActivity.displayErrorToast("Unable to retrieve beacons");
//            }
//        mainActivity.setMeetingStartedFlag();
//            meetingStarted = true;
        }

    public void startBeaconing() {
        Log.d(TAG, "Bootstrapping Regions!");
        beaconManager.bind(this);
        try {
            bootstrapRegions(regions);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendWelcomeNotification(String tempUUID) {
        Log.d(TAG, "Sending Welcome Notification");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("DeVry Education Group")
                        .setContentText("DeVry would like to welcome you!")
                        .setSmallIcon(R.drawable.ic_dvg)
                        .setColor(getResources().getColor(R.color.devryGold))
                        .setAutoCancel(true);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("extraType", "welcome");
        resultIntent.putExtra("beaconUUID", tempUUID);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
//        NotificationManager notificationManager =
//                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void sendFeedbackNotification(Map<String, String> extras) {
        Log.d(TAG, "Sending Feedback Notification");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("DeVry Education Group")
                        .setContentText("DeVry would like your feedback!")
                        .setSmallIcon(R.drawable.ic_dvg)
                        .setColor(getResources().getColor(R.color.devryGold))
                        .setAutoCancel(true);

        Intent resultIntent = new Intent(this, MainActivity.class);
        if(extras != null) {
            for (Map.Entry<String, String> entry : extras.entrySet())
            {
                System.out.println(entry.getKey() + "/" + entry.getValue());
                resultIntent.putExtra(entry.getKey(), entry.getValue());
            }
        }
        resultIntent.putExtra("extraType", "feedback");
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
//        NotificationManager notificationManager =
//                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void setUUID(String resumedUUID) {
        userInitialUUID = resumedUUID;
    }

    public void setUserWithinRange(boolean tempbool){
        userWithinRange = tempbool;
    }

    public void sendReminderNotification() {
        Log.d(TAG, "Sending Reminder Feedback");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("DeVry Education Group")
                        .setContentText("The meeting is starting soon...")
                        .setSmallIcon(R.drawable.ic_dvg)
                        .setColor(getResources().getColor(R.color.devryGold))
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setAutoCancel(true);
//        NotificationManager notificationManager =
//                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    public void stopAllServiceConnections() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("reminders");
        FirebaseMessaging.getInstance().unsubscribeFromTopic("feedback");
//        if(regionBootstrap != null) {
//            regionBootstrap.disable();
//        }
        Log.d(TAG, "regionBootstrap was empty");
        removeAllBeaconNotifiers();
    }

    public void initializeBeaconManager() {
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setBackgroundBetweenScanPeriod(5000l);
        beaconManager.setForegroundBetweenScanPeriod(2000l);
    }

    public void clearNotifications() {
        notificationManager.cancelAll();
    }

}