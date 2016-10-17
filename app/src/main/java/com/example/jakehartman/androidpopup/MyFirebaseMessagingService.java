package com.example.jakehartman.androidpopup;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by ernesto on 10/17/16.
 *
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("firebase", "From: " + remoteMessage.getFrom());

        //check for data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d("firebase", "message data payload : " + remoteMessage.getData());
        }

        // check for notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d("firebase", "message notification body: " + remoteMessage.getNotification().getBody());

        }

    }
}
