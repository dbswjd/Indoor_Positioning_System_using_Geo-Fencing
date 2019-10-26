package com.example.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class RegisterActivity extends Activity {

    private static final int INFO_REGISTERED = 1010;
    private boolean serverConnected = false;
    private EditText mName,mPhone,mEmail;
//    private String REQUEST_URL = "http://54.167.123.220:3333";
    private String REQUEST_URL = "http://164.125.69.170:3335";
    private Button btnSubmit;
    private BackPressCloseHandler backPressCloseHandler;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        backPressCloseHandler = new BackPressCloseHandler(this);

        Intent intent = getIntent();
        final String name = intent.getExtras().getString("name");
        final String phone = intent.getExtras().getString("phone");
        final String email = intent.getExtras().getString("email");

        mName = (EditText) findViewById(R.id.name);
        mName.setText(name);
        mPhone = (EditText)findViewById(R.id.phonenumber);
        mPhone.setText(phone);
        mEmail = (EditText)findViewById(R.id.email);
        mEmail.setText(email);
        btnSubmit = (Button) findViewById(R.id.SubmitButton);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if( mName.getText().toString().length() == 0  ) {
                    Toast.makeText(RegisterActivity.this, "이름을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if( mPhone.getText().toString().length() == 0 ) {
                    Toast.makeText(RegisterActivity.this,"- 를 제외한 휴대폰 번호을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    if(!Pattern.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", mPhone.getText().toString())) {
                        Toast.makeText(RegisterActivity.this,"올바른 휴대폰 번호가 아닙니다.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if( mEmail.getText().toString().length() == 0 ) {
                    Toast.makeText(RegisterActivity.this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
                    if(!android.util.Patterns.EMAIL_ADDRESS.matcher( mEmail.getText().toString()).matches()){
                        Toast.makeText(RegisterActivity.this,"올바른 이메일 형식이 아닙니다",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

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
            Intent intent = new Intent(RegisterActivity.this,MainActivity.class);
            intent.putExtra("name",mName.getText().toString());
            intent.putExtra("phone",mPhone.getText().toString());
            intent.putExtra("email",mEmail.getText().toString());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(intent,2000);
        }
        else{
            Toast toast = Toast.makeText(RegisterActivity.this,
                    "서버와 연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
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


}
