package com.example.myapplication;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

public class BeaconReferenceApplication extends Application implements BootstrapNotifier {
        private static final String TAG = "BeaconReferenceApp";
        private RegionBootstrap regionBootstrap;
        private BackgroundPowerSaver backgroundPowerSaver;
        private boolean haveDetectedBeaconsSinceBoot = false;
        private MainActivity mainActivity = null;
        private String cumulativeLog = "";
        private boolean state = false;

        public void onCreate() {
            super.onCreate();

            BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
            beaconManager.setDebug(false);

            Log.d(TAG, "setting up background monitoring for beacons and power saving");

            // wake up the app when a beacon is seen
            Region region = new Region("backgroundRegion",
                    null, null, null);
            regionBootstrap = new RegionBootstrap(this, region);

            // simply constructing this class and holding a reference to it in your custom Application
            // class will automatically cause the BeaconLibrary to save battery whenever the application
            // is not visible.  This reduces bluetooth power usage by about 60%
            backgroundPowerSaver = new BackgroundPowerSaver(this);

        }

//        public void disableMonitoring() {
////            if (regionBootstrap != null) {
////                regionBootstrap.disable();
////                regionBootstrap = null;
////            }
////        }
////
////        public void enableMonitoring() {
////            Region region = new Region("backgroundRegion", null, null, null);
////            regionBootstrap = new RegionBootstrap(this, region);
////        }


        @Override
        public void didEnterRegion(Region arg0) {
            // In this example, this class sends a notification to the user whenever a Beacon
            // matching a Region (defined above) are first seen.
            Log.d(TAG, "did enter region.");
            if (!haveDetectedBeaconsSinceBoot) {
                Log.d(TAG, "auto launching MainActivity");

//                Intent intent = new Intent(this, MonitoringActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                // Important:  make sure to add android:launchMode="singleInstance" in the manifest
//                // to keep multiple copies of this activity from getting created if the user has
//                // already manually launched the app.
//                this.startActivity(intent);

                DisplayState(true);
                haveDetectedBeaconsSinceBoot = true;
            }

            else {
                if (mainActivity != null) {
                    // If the Monitoring Activity is visible, we log info about the beacons we have
                    // seen on its display
                DisplayState(true);
                } else {
                    // If we have already seen beacons before, but the monitoring activity is not in
                    // the foreground, we send a notification to the user on subsequent detections.
                    Log.d(TAG, "Sending notification.");
                    sendNotification();
                }
            }


        }

        @Override
        public void didExitRegion(Region region) {
            DisplayState(false);
        }


        @Override
        public void didDetermineStateForRegion(int state, Region region) {
            if(state == 1)  DisplayState(true);
            else DisplayState(false);
        }

        private void DisplayState(boolean flag){
            state = flag;
            if(this.mainActivity != null){
                this.mainActivity.updateMonitoringState(flag);
            }
        }

        private void sendNotification() {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setContentTitle("Beacon Reference Application")
                            .setContentText("An beacon is nearby.")
                            .setSmallIcon(R.drawable.ic_launcher_background);

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

        public void setMainActivity(MainActivity activity) {
            this.mainActivity = activity;
        }

        public boolean getState() {
            return state;
        }

    }

