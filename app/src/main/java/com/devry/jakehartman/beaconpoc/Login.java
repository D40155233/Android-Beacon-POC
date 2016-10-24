package com.devry.jakehartman.beaconpoc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Login extends AppCompatActivity {

    private BeaconReferenceApplication app;
    EditText colleagueIDEntry;
    String ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String TAG = "LoginActivity";
//        if (!isTaskRoot()) {
//            finish();
//            return;
//        }
        setContentView(R.layout.activity_login);
        app = (BeaconReferenceApplication)getApplication();
        colleagueIDEntry = (EditText) findViewById(R.id.colleagueID);
        Button submitButton = (Button) findViewById(R.id.buttonSubmit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do service call here to validate D#
                ID = colleagueIDEntry.getText().toString();
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("http://httpstat.us/200")
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    //If Service Call Bad:

                    @Override
                    public void onFailure(Call call, IOException e) {
                        Toast.makeText(Login.this, "Invalid D#", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "D# was not valid");
                    }
                    //If Service Call Good:

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Log.d(TAG, "ID Before: " + ID);
//                                SharedPreferences.Editor editor = getSharedPreferences("sharedPreferencesData", MODE_PRIVATE).edit();
//                                editor.putString("userID", ID);
//                                editor.putBoolean("loginFlag", false);
//                                editor.commit();
                                Log.d(TAG, "ID After: " + ID);
                                app.bindBeacon(ID);
                                Intent intent = new Intent(Login.this, MainActivity.class);
                                intent.putExtra("extraType", "login");
                                intent.putExtra("userID", ID);
                                startActivity(intent);
                            }
                        });
                    }
                });
            }
        });
    }

}
