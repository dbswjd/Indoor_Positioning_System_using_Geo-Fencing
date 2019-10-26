package com.example.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.angads25.toggle.LabeledSwitch;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import io.socket.client.Socket;
import io.socket.client.IO;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity implements BeaconConsumer, SensorEventListener {

//    private String REQUEST_URL = "http://54.167.123.220:3335";
    private String REQUEST_URL = "http://164.125.69.170:3335";
    private Socket mSocket;
    private Button registerBtn = null;
    private Button rangingBtn = null;
    private boolean beaconConnected = false;
    private TextView Info;
    private BackPressCloseHandler backPressCloseHandler;
    private BeaconManager beaconManager;
    private Vibrator vibrator;

    private SensorManager sensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mLinearAccelerometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private float[] mLastLinearAccelerometer = new float[3];
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private final String[] Direction = {"N","W","S","E"};

    private boolean mLastAccelererometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private boolean isSensorRunning = false;
    private String curDirection ="";
    private int prevIndex = -1;

    private Kalman mKalmanAccX;
    private Kalman mKalmanAccY;
    private Kalman mKalmanAccZ;

    private float offsetY, offsetX, offsetZ;
    private boolean offset_set = false;
    private int mSensorChangedCounter = 0;

    private double mCurrentAccel, mPreviousAccel=-1;

    private double maxTotal = 0.8, minTotal = -0.8;
    private double deltaPositive = 0.33, deltaNegative = -0.33;

    private int mSteps = 0;
    private boolean isPositive = true;

    private int rangingButtonClicked  = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        backPressCloseHandler = new BackPressCloseHandler(this);

        Intent intent = getIntent();
        Info = (TextView)findViewById(R.id.Info);
        final String name = intent.getExtras().getString("name");
        final String phone = intent.getExtras().getString("phone");
        final String email = intent.getExtras().getString("email");
        Log.d("info",phone);
        Info.setText("안녕하세요\n"+name+"님");

        registerBtn = (Button)findViewById(R.id.registerButton);
        rangingBtn = (Button)findViewById(R.id.rangingButton);

        ImageView imageTV = (ImageView)findViewById(R.id.iv);
        Glide.with(this).load(R.drawable.location_marker).diskCacheStrategy(DiskCacheStrategy.SOURCE).into(imageTV);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
        updateMonitoringState(false);

        try {
            beaconManager.setForegroundScanPeriod(400l);
            beaconManager.updateScanPeriods();
        }catch(Exception e) {
            Log.d("error : scan time", e.toString());
        }

        try{
            mSocket = IO.socket(REQUEST_URL);
            mSocket.on(Socket.EVENT_CONNECT,onConnect);
            mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
            mSocket.on("alert",onAlert);
            mSocket.connect();
        }
        catch(URISyntaxException e){
            Log.d("error",e.toString());
        }

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mLinearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mKalmanAccX = new Kalman(0.0f);
        mKalmanAccY = new Kalman(0.0f);
        mKalmanAccZ = new Kalman(0.0f);

        isSensorRunning = true;

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, RegisterActivity.class);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                myIntent.putExtra("name",name);
                myIntent.putExtra("phone",phone);
                myIntent.putExtra("email",email);
                startActivity(myIntent);
            }
        });

        rangingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrator.vibrate(1000);
                Toast toast = Toast.makeText(MainActivity.this,
                        "위치 측정 중입니다. ", Toast.LENGTH_SHORT);
                toast.show();

                if(rangingButtonClicked == 0 ){
                    updateMonitoringState(true);
                }
                else {
                    rangingButtonClicked++;
                }
                Log.d("eror",String.valueOf(rangingButtonClicked));
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT,onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT,onDisconnect);
        isSensorRunning = false;
        beaconConnected = false;
        sensorManager.unregisterListener(this, mAccelerometer);
        sensorManager.unregisterListener(this, mMagnetometer);
        sensorManager.unregisterListener(this,mLinearAccelerometer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.unbind(this);

        isSensorRunning = false;
        beaconConnected = false;

        sensorManager.unregisterListener(this, mAccelerometer);
        sensorManager.unregisterListener(this, mMagnetometer);
        sensorManager.unregisterListener(this,mLinearAccelerometer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        beaconManager.bind(this);

        isSensorRunning = true;
        mSteps = 0;
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this,mLinearAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }


    @Override public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
}

    private Emitter.Listener onConnect = new Emitter.Listener(){
        @Override
        public void call(Object... args){
            Log.d("connection","서버 연결 중");
        }
    };


    private Emitter.Listener onDisconnect = new Emitter.Listener(){
        @Override
        public void call(Object... args){
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(),"서버와의 연결이 끊어졌습니다.",Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Emitter.Listener onAlert = new Emitter.Listener(){
        @Override
        public void call(Object... args){
            Log.d(">>>room",String.valueOf(args[0]));
            vibrator.vibrate(1000);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    String text = "현재 이 곳은 사용이 불가능 합니다.";
                    builder.setTitle("알림").setMessage(text);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                        }
                    });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            });
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000 && resultCode == RESULT_OK) {
            Info.setText("안녕하세요\n"+data.getStringExtra("name")+"님");
        }
    }

    public void updateMonitoringState(final boolean flag) {
        runOnUiThread(new Runnable() {
            public void run() {
                if(flag) {
                    rangingBtn.setText("위치 측정 중");
                    rangingBtn.setEnabled(true);
                    rangingBtn.setBackground(getDrawable(R.drawable.button_states));
                    beaconConnected = true;
                }
                else{
                    rangingBtn.setText("비콘 탐색 중");
                    rangingBtn.setBackground(getDrawable(R.drawable.button_state_error));
                    rangingBtn.setEnabled(false);
                    beaconConnected = false;
                    rangingButtonClicked = 0 ;
                }
            }
        });
    }

    @Override
    public void onBeaconServiceConnect() {

        RangeNotifier rangeNotifier = new RangeNotifier() {

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {


                if (beacons.size() > 0 ) {

                    updateMonitoringState(true);

                    String out= "";
                    JSONArray arr = new JSONArray();
                    Iterator it = beacons.iterator();

                    while (it.hasNext()) {
                        Beacon thisBeacon = (Beacon) it.next();
                        int thisRSSI = thisBeacon.getRssi();
                        int thisID = thisBeacon.getId3().toInt();
                        if ((thisID <= 4) || (thisID >= 13)) continue;

                        Long now = System.currentTimeMillis();
                        Date dateNow = new Date(now);

                        JSONObject obj = new JSONObject();

                        try {
                            obj.put("id", String.valueOf(thisID));
                            obj.put("rssi", String.valueOf(thisRSSI));
                            obj.put("time", String.valueOf(dateNow));
                            out += thisID + "  " + thisRSSI + "  " + dateNow+"\n";
                            arr.put(obj);
                        } catch (Exception e) {
                            Log.d("error: json object", e.toString());
                        }
                    }

                    JSONObject jsonobject = new JSONObject();
                    try {
                        jsonobject.put("data", arr);
                    }
                    catch(Exception e){
                        Log.i("error: json array",e.toString());
                    }

                    final String res = jsonobject.toString();
                    mSocket.emit("beaconData", res);
                    //sendData(res);
                }

            }
        };

        try {

            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));

            //beaconManager.startRangingBeaconsInRegion(new Region("88888888-4444-4444-4444-0000000000000000", Identifier.fromInt(8109), Identifier.fromInt(1), null));
            beaconManager.addRangeNotifier(rangeNotifier);
            //beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            //beaconManager.startRangingBeaconsInRegion(new Region("abcdefab-aaaa-bbbb-cccc-123456789012", Identifier.fromInt(1000), Identifier.fromInt(5), null));
            //beaconManager.startRangingBeaconsInRegion(new Region("abcdefab-aaaa-bbbb-cccc-123456789011", null, null, null));
            //beaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {   }
    }
//
//    public void sendData(final String data) {
//        Thread thread = new Thread(new Runnable() {
//            public void run() {
//
//                try {
//                    URL url = new URL(REQUEST_URL);
//                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//
//                    conn.setConnectTimeout(2000);
//                    conn.setReadTimeout(2000);
//                    conn.setRequestMethod("POST");
//                    conn.setRequestProperty("Content-Type", "application/json");
//                    conn.setRequestProperty("Accept", "text/html");
//                    conn.setDoOutput(true);
//                    conn.setDoInput(true);
//                    conn.connect();
//
//                    OutputStream os = conn.getOutputStream();
//                    os.write(data.getBytes("utf-8"));
//                    Log.d("결과",data);
//                    os.flush();
//
//                    InputStream inputStream;
//                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                        inputStream = conn.getInputStream();
//                    } else {
//                        inputStream = conn.getErrorStream();
//                        finish();
//                    }
//
//                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
//                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//
//                    while (bufferedReader.readLine() != null) {
//                        Log.d("input: ",bufferedReader.readLine());
//                    }
//
//                    bufferedReader.close();
//                    conn.disconnect();
//
//                } catch (Exception e) {
//                    Log.i("error: server connect", e.toString());
//                }
//            }
//        });
//        thread.start();
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelererometerSet = true;
        }

        if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }

        if (mLastAccelererometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            int azimuthinDegrees = (int) (Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0]) + 360) % 360;

            int index = (( azimuthinDegrees+45)/90)%4;
            curDirection = Direction[index];

            if(prevIndex != index){
                prevIndex = index;
            }
        }


        if( event.sensor == mLinearAccelerometer ){
            System.arraycopy(event.values, 0, mLastLinearAccelerometer, 0, event.values.length);
        }

        if(beaconConnected) {

            //보정해용//
            float filteredX = (float) mKalmanAccX.update(mLastLinearAccelerometer[0]);
            float filteredY = (float) mKalmanAccY.update(mLastLinearAccelerometer[1]);
            float filteredZ = (float) mKalmanAccZ.update(mLastLinearAccelerometer[2]);

            mLastLinearAccelerometer[0] = filteredX;
            mLastLinearAccelerometer[1] = filteredY;
            mLastLinearAccelerometer[2] = filteredZ;

            //offset 등록//
            if (offset_set == false) {
                offsetX = mLastLinearAccelerometer[0];
                offsetY = mLastLinearAccelerometer[1];
                offsetZ = mLastLinearAccelerometer[2];
            }
            offset_set = true;

            mSensorChangedCounter++;
            mCurrentAccel = mLastLinearAccelerometer[1] - offsetY + mLastLinearAccelerometer[2] - offsetZ;
            CalculateSteps();
        }
    }

    public void CalculateSteps(){

        //처음에 핸드폰 움직여서 작게 왔다갔다 하는 것들 없애기//
        if(mCurrentAccel * mPreviousAccel < 0){
            mSensorChangedCounter = 0;
        }
        else{
            mSensorChangedCounter++;
        }

        mPreviousAccel = mCurrentAccel;

        if(mSensorChangedCounter < 20 )    return;

        //작은건 노이즈 처리
        if(mCurrentAccel> 0  && mCurrentAccel < deltaPositive)    return;
        if(mCurrentAccel< 0 && mCurrentAccel > deltaNegative)    return;


        // 기준치 값 update//
        if(maxTotal < mCurrentAccel) {
            maxTotal = mCurrentAccel;
            deltaPositive = maxTotal / 4;
        }

        if(minTotal >  mCurrentAccel) {
            minTotal = mCurrentAccel;
            deltaNegative = minTotal / 4;
        }

        // 양수로 기준점찍고 다시 음수 기준보다 작아지면 1걸음으로 판별//
        if(isPositive){
            if(mCurrentAccel < deltaNegative){
                isPositive = false;
                mSteps++;

                Log.d("걸음",String.valueOf(mSteps));

                JSONObject jsonobject = new JSONObject();
                JSONObject obj = new JSONObject();
                try {
                    jsonobject.put("direction",curDirection);
                    jsonobject.put("step",mSteps);
                    obj.put("movement",jsonobject);
                }
                catch(Exception e){
                    Log.i("error: json array",e.toString());
                }

                final String res = obj.toString();
                mSocket.emit("stepData", res);

            }
        }
        else{
            if(mCurrentAccel >  deltaPositive){
                isPositive = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
