package com.dvg.jakehartman.beaconpoc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.vdurmont.emoji.EmojiParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Login extends AppCompatActivity {

    private BeaconReferenceApplication app;
    //Set this variable to true if you want to bypass authentication regardless of errors from D# validation service from backend.
    private final boolean BYPASS_LOGIN = false;
    EditText colleagueIDEntry;
    String ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String TAG = "LoginActivity";
        setContentView(R.layout.activity_login);
        app = (BeaconReferenceApplication)getApplication();
        final Button submitButton = (Button) findViewById(R.id.buttonSubmit);
        colleagueIDEntry = (EditText) findViewById(R.id.colleagueID);
        colleagueIDEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(colleagueIDEntry.getText().length() == 9) {
                    submitButton.setEnabled(true);
                }
                else {
                    submitButton.setEnabled(false);
                }
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do service call here to validate D#
                ID = colleagueIDEntry.getText().toString().toUpperCase();
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://ec2-52-44-53-47.compute-1.amazonaws.com:8080/DVG-CustomerEngagement-Services/api/customerengagement/user/" + ID)
                        .addHeader("authorization", "PYJIKS17nR1rjB+RroyU/KzgUmoz9x84r9YehdpLhJw=")
                        .addHeader("dsi", "D40234627")
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    //If Service Call Bad:

                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (BYPASS_LOGIN == true) {
                                    app.bindBeacon(ID);
                                    Intent intent = new Intent(Login.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("extraType", "login");
                                    intent.putExtra("userID", ID);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(Login.this, "D# validation failed", Toast.LENGTH_LONG).show();
                                }

                            }
                        });
                        Log.d(TAG, "D# validation failed");
                    }
                    //If Service Call Good:

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String resp = response.body().string();
                        String tempDSI = null;
                        resp = EmojiParser.removeAllEmojis(resp);
                        Log.d(TAG, resp);
                        try {
                            JSONObject json = new JSONObject(resp);
                            tempDSI = json.getString("dsi");
                            Log.d(TAG, tempDSI);
                            if (!tempDSI.equals("null")) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        app.bindBeacon(ID);
                                        Intent intent = new Intent(Login.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        intent.putExtra("extraType", "login");
                                        intent.putExtra("userID", ID);
                                        startActivity(intent);
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(Login.this, "DSI was not valid. Please try again", Toast.LENGTH_LONG).show();
                                        Log.d(TAG, "D# returned from login service was null");
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (BYPASS_LOGIN == true) {
                                        app.bindBeacon(ID);
                                        Intent intent = new Intent(Login.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        intent.putExtra("extraType", "login");
                                        intent.putExtra("userID", ID);
                                        startActivity(intent);
                                    } else {
                                    Toast.makeText(Login.this, "DSI was not valid. Please try again", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }

                    }
                });
            }
        });
    }

}
