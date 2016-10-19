package com.example.jakehartman.androidpopup;

import android.content.Intent;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

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
            Log.d("firebase", "message data payload : " + remoteMessage.getData());
            app = (BeaconReferenceApplication)getApplication();
            app.feedbackInfoReceived(remoteMessage.getData());
        }

        // check for notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d("firebase", "message notification body: " + remoteMessage.getNotification().getBody());

        }

    }
}
