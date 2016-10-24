package com.devry.jakehartman.beaconpoc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.BeaconManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "DVGBeacon";
    public boolean showFeedbackOnResume = false;
    private static final String TAG = "BeaconReferenceAppF";

    private Context context = this;
    private BeaconReferenceApplication app;
    private BeaconManager beaconManager;
    private Dialog welcomeDialog;
    private Dialog feedbackDialog;
    private Dialog dialog;
    private SharedPreferences sharedPreferences;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    //flags
    private Boolean loginFlag = false;
    private Boolean meetingStartedFlag = false;
    private Boolean beaconFoundFlag = false;
    private String UUID = "None";
    private String userID = "Unknown";
    @TargetApi(23)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = context.getSharedPreferences("sharedPreferencesData", Context.MODE_PRIVATE);
        Log.d(TAG, "Getting Preferences...");
        loginFlag = sharedPreferences.getBoolean("loginFlag", false);
        meetingStartedFlag = sharedPreferences.getBoolean("meetingStartedFlag", false);
        beaconFoundFlag = sharedPreferences.getBoolean("beaconFoundFlag", false);
        UUID = sharedPreferences.getString("UUID", "None");
        userID = sharedPreferences.getString("userID", "Unknown");
        setContentView(R.layout.activity_main);
        app = (BeaconReferenceApplication)getApplication();
        welcomeDialog = new Dialog(context);
        feedbackDialog = new Dialog(context);
        dialog = new Dialog(context);
        Button debugButton = (Button) findViewById(R.id.debugButton);
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            userID = extras.getString("userID");
            if(extras.getString("extraType").equals("login")) {
                loginFlag = true;
            }
        }
        updateDebugValues();
        debugButton.setVisibility(View.VISIBLE);

        debugButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                LinearLayout debugLayout = (LinearLayout) findViewById(R.id.debugLayout);
                if (debugLayout.getVisibility() == debugLayout.INVISIBLE)
                    debugLayout.setVisibility(debugLayout.VISIBLE);
                else if (debugLayout.getVisibility() == debugLayout.VISIBLE)
                    debugLayout.setVisibility(debugLayout.INVISIBLE);
                //debugLayout.setVisibility(LinearLayout.VISIBLE);
                return true;
                //Toast.makeText(MainActivity.this, "Values Reset", Toast.LENGTH_LONG).show();
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d(TAG, "Set new Intent");
        Log.d(TAG, "Stuff: " + intent.getStringExtra("stuff"));
        Bundle extras = intent.getExtras();
        Log.d(TAG, "Checking for extras...");
        if (extras != null) {
            Log.d(TAG, "EXTRAS!!!");
            String extraType = extras.getString("extraType");
            Log.d(TAG, "Extra type: " + extraType);
            if (extraType.equals("login")) {
                if(extras.getBoolean("loginFlag") == true) {
                    loginFlag = true;
                    updateDebugValues();
                }
            }
            else if(extraType.equals("feedback")) {
                displayFeedbackPopup(extras.getString("questionText"), extras.getString("questionNumber"));
            }
            else if(extraType.equals("welcome")) {
                displayWelcomePopup();
            }
        }
        else {
            Log.d(TAG, "NOOOO EXTRAS!!!");
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
        ((BeaconReferenceApplication) this.getApplicationContext()).setMainActivity(this);
        app.setUUID(UUID);
        updateDebugValues();
        if(meetingStartedFlag == true && UUID == "Unknown") {
            app.startBeaconing();
        }
        Log.d(TAG, "Main Activity in foreground!!!!!");

    }

    @Override
    public void onPause() {
        super.onPause();
        ((BeaconReferenceApplication) this.getApplicationContext()).setMainActivity(null);
        SharedPreferences.Editor editor = getSharedPreferences("sharedPreferencesData", MODE_PRIVATE).edit();
        editor.putBoolean("loginFlag", loginFlag);
        editor.putBoolean("meetingStartedFlag", meetingStartedFlag);
        editor.putBoolean("beaconFoundFlag", beaconFoundFlag);
        editor.putString("UUID", UUID);
        editor.putString("userID", userID);
        editor.commit();
        Log.d(TAG, "PAUSED!!!!!");
    }

    public void displayWelcomePopup() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
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
                        app.sendAttendeeData();
                        app.setShowWelcomeOnResume(false);
                    }
                });
            }
        });
    }

    public void displayFeedbackPopup(final String question, final String questionNumber) {
        Log.d(TAG, "Trying to display popup...");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContentView(R.layout.feedback_popup);
                dialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
                dialog.show();

                String fullQuestionString = question;

                TextView questionText = (TextView) dialog.findViewById(R.id.questionText);
                questionText.setText((CharSequence) fullQuestionString);

                Button dialogButtonOK = (Button) dialog.findViewById(R.id.dialogButtonOK);

                dialogButtonOK.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RatingBar feedbackRating = (RatingBar) dialog.findViewById(R.id.feedbackRating);
                        EditText feedbackComment = (EditText) dialog.findViewById(R.id.feedbackComment);
                        int fbr = (int)feedbackRating.getRating();
                        String fbc = feedbackComment.getText().toString();
                        app.sendFeedbackData(questionNumber, fbr, fbc);
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    public void displayErrorToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void updateDebugValues() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView loginFlagValue = (TextView) findViewById(R.id.loginFlagValue);
                TextView meetingStartedValue = (TextView) findViewById(R.id.meetingStartValue);
                TextView beaconFoundValue = (TextView) findViewById(R.id.beaconFoundValue);
                TextView beaconNameValue = (TextView) findViewById(R.id.beaconNameValue);
                TextView userIDValue = (TextView) findViewById(R.id.userIDValue);

                loginFlagValue.setText((CharSequence) (loginFlag.toString()));
                meetingStartedValue.setText((CharSequence) meetingStartedFlag.toString());
                beaconFoundValue.setText((CharSequence) beaconFoundFlag.toString());
                beaconNameValue.setText((CharSequence) UUID);
                userIDValue.setText((CharSequence) userID);
            }
        });
    }

    public void setBeaconFoundFlag(String beaconID) {
        beaconFoundFlag = true;
        UUID = beaconID;
        updateDebugValues();
    }

    public void setMeetingStartedFlag() {
        meetingStartedFlag = true;
        updateDebugValues();
    }


}