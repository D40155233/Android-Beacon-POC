package com.example.jakehartman.androidpopup;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.Activity;

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

import java.util.Collection;

public class BeaconReferenceApplication extends Application implements BootstrapNotifier, BeaconConsumer, RangeNotifier {
    private static final String TAG = "BeaconReferenceApp";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private MainActivity rangingActivity = null;
    private boolean showFeedbackOnResume = false;
    public static final String PREFS_NAME = "DVGBeacon";
    private boolean haveDetectedBeaconsSinceBoot = false;
    private boolean welcomePopupShown = false;

    BeaconManager beaconManager;


    public void onCreate() {
        super.onCreate();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        Region region = new Region("backgroundRegion", Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"), Identifier.parse("0"), Identifier.parse("0"));
        regionBootstrap = new RegionBootstrap(this, region);

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        beaconManager.setBackgroundBetweenScanPeriod(1100l);
        //beaconManager.setForegroundBetweenScanPeriod(2000l);
        beaconManager.bind(this);
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
            if (!haveDetectedBeaconsSinceBoot && beacon.getDistance()<1) {
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
                        rangingActivity.displayWelcomePopup();
                        welcomePopupShown = true;
                    }
                } else if (rangingActivity == null && beacon.getDistance()<5){
                    // If we have already seen beacons before, but the monitoring activity is not in
                    // the foreground, we send a notification to the user on subsequent detections.
                    if(welcomePopupShown == false) {
                        Log.d(TAG, "Sending notification.");
                        sendNotification("DeVry Education Group would like to welcome you!");
                    }
                }
                //This is the implementation of showing the feedbackPopup, based upon the assumption that
                //we will display it once the user has seen the welcome popup and moved x meters away from it
                //Delete if logic changes
                if (rangingActivity != null && beacon.getDistance()>10 && welcomePopupShown == true) {
                    Log.d(TAG, "Showing Feedback popup!");
                    rangingActivity.displayFeedbackPopup();
                    try {
                        beaconManager.stopRangingBeaconsInRegion(region);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (rangingActivity == null && beacon.getDistance()>10 && welcomePopupShown == true){
                    // If we have already seen beacons before, but the monitoring activity is not in
                    // the foreground, we send a notification to the user on subsequent detections.
                    Log.d(TAG, "Sending notification.");
                    sendNotification("Please provide feedback for DeVry Education Group's IT Update Meeting!");
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

    public boolean isShowFeedbackOnResume() {
        return showFeedbackOnResume;
    }
}