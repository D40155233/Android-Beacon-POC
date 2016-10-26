package com.dvg.jakehartman.beaconpoc;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by ernesto on 10/17/16.
 *
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private BeaconReferenceApplication app;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("firebase", "From: " + remoteMessage.getFrom());

        //check for data payload
        if (remoteMessage.getData().size() > 0) {
            app = (BeaconReferenceApplication)getApplication();
            if(remoteMessage.getFrom().equals("/topics/feedback")) {
                app.feedbackInfoReceived(remoteMessage.getData());
            }
            else if(remoteMessage.getFrom().equals("/topics/reminders")) {
                Log.d("firebase", "sending payload to app");
                app.reminderInfoReceived(remoteMessage.getData());
            }
        }

        // check for notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d("firebase", "message notification body: " + remoteMessage.getNotification().getBody());

        }

    }
}
