package com.example.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import org.altbeacon.beacon.BeaconManager;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class InitialActivity extends Activity {
    private EditText mName,mPhone,mEmail;
    private Button btnSubmit;
    private String REQUEST_URL = "http://164.125.69.170:3333";

    //    private String REQUEST_URL = "http://54.167.123.220:3333";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int INFO_REGISTERED = 1010;
    private boolean serverConnected = false;
    private MainActivity mainActivity = null;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        mName = (EditText) findViewById(R.id.name);
        mPhone = (EditText)findViewById(R.id.phonenumber);
        mEmail = (EditText)findViewById(R.id.email);
        btnSubmit = (Button) findViewById(R.id.SubmitButton);

        verifyBluetooth();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons in the background.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if( mName.getText().toString().length() == 0  ) {
                    Toast.makeText(InitialActivity.this, "이름을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if( mPhone.getText().toString().length() == 0 ) {
                    Toast.makeText(InitialActivity.this,"- 를 제외한 휴대폰 번호을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    if(!Pattern.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", mPhone.getText().toString())) {
                        Toast.makeText(InitialActivity.this,"올바른 휴대폰 번호가 아닙니다.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if( mEmail.getText().toString().length() == 0 ) {
                    Toast.makeText(InitialActivity.this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    if(!android.util.Patterns.EMAIL_ADDRESS.matcher( mEmail.getText().toString()).matches()){
                        Toast.makeText(InitialActivity.this,"올바른 이메일 형식이 아닙니다",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Toast toast = Toast.makeText(InitialActivity.this,
                        "정보 수정이 완료되었습니다.", Toast.LENGTH_SHORT);
                toast.show();

                JSONObject obj = new JSONObject();
                JSONObject jsonObject = new JSONObject();

                try {
                    obj.put("name",mName.getText().toString());
                    obj.put("phone", mPhone.getText().toString());
                    obj.put("contact", mEmail.getText().toString() );
                    jsonObject.put("Initinfo",obj);
                } catch (Exception e) {
                    Log.d("error: json object", e.toString());
                }

                sendData(jsonObject.toString());

            }

        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("initial acitivity", "coarse location permission granted");
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

    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == INFO_REGISTERED) {
                serverConnected = true;
            }
            else{
                serverConnected = false;
            }
            moveToMain();
        }
    };


    private void moveToMain(){
        if( serverConnected ){
            Log.d("server connection","true");
            Intent intent = new Intent(InitialActivity.this,MainActivity.class);
            intent.putExtra("name",mName.getText().toString());
            intent.putExtra("phone",mPhone.getText().toString());
            intent.putExtra("email",mEmail.getText().toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(intent,2000);
        }
        else{
            Toast toast = Toast.makeText(InitialActivity.this,
                    "서버와 연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    public void sendData(final String data) {

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(REQUEST_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "text/html");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.connect();

                    OutputStream os = conn.getOutputStream();
                    os.write(data.getBytes("utf-8"));
                    os.flush();
                    os.close();

                    InputStream inputStream;
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        inputStream = conn.getInputStream();
                    } else {
                        inputStream = conn.getErrorStream();
                        finish();
                    }


                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);


                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while((line = bufferedReader.readLine()) != null){
                        buffer.append(line);
                    }

                    String res = buffer.toString();

                    if(res.equals("registered")){
                        Message m = new Message();
                        m.what = INFO_REGISTERED;
                        m.obj= "done";
                        handler.sendMessage(m);
                    }

                    bufferedReader.close();
                    conn.disconnect();

                } catch (Exception e) {
                    Log.i("error: server connect", e.toString());
                }
            }
        });
        thread.start();
    }


    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //finish();
                        //System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    //finish();
                    //System.exit(0);
                }

            });
            builder.show();

        }

    }

}
