package com.mabe.productions.pr_ipulsus_running;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;


public class GoogleMapService extends Service {

    private static final int ID_SERVICE = 1234;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private String channelId;

    public static boolean isLocationListeningEnabled = false;
    public static final String ACTION_SEND_GPS_DATA = "SEND_GPS_DATA";
    public static final String ACTION_SEND_GPS_AVAILABILITY_DATA = "SEND_GPS_AVAILABILITY_DATA";
    public static final String GPS_DATA = "LOCATION_RESULT";
    public static final String LOCATION_AVAILABILITY_DATA = "LOCATION_AVAILABILITY";
    public static final String GPS_WAKELOCK_TAG = "HRV_MADISON_GPS";
    private FusedLocationProviderClient mFusedLocationClient;
    private KeyguardManager myKM;
    private ArrayList<LocationResult> locationArrayList = new ArrayList<>();
    private PowerManager.WakeLock wakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();


        Context context = this;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        notificationBuilder = new NotificationCompat.Builder(context,channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_appicon_heart)
                .setPriority(PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentText("Working out")
                .setChannelId(channelId)
                .build();

        startForeground(ID_SERVICE, notification);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //noinspection MissingPermission
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);

        //noinspection MissingPermission
//        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
//                                                    locationPendingIntent);


        myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }


    private void aquireWakelock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                GPS_WAKELOCK_TAG);
        wakeLock.acquire();
    }

    private void releaseWakelock() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        aquireWakelock();
        return START_NOT_STICKY;
    }

    //The location callback that stores paceData and distance.
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            //If accuraccy is shit do not add to locationResults Array
            if (locationResult.getLastLocation().getAccuracy() > 50) {
                return;
            }
            //If accuraccy is good add to locationResults Array
            locationArrayList.add(locationResult);

            //If screen is locked, keep on adding and don't send any data
            if (myKM.inKeyguardRestrictedInputMode()) {
                //it is locked
                return;
            }

            //When screen is not locked anymore - send data chunk to workoutFragment.class
            LocalBroadcastManager.getInstance(GoogleMapService.this).sendBroadcast(new Intent(ACTION_SEND_GPS_DATA).putExtra(GPS_DATA, locationArrayList.toArray(new LocationResult[0])));

            //Clear arrayList after sending, to start adding new data
            locationArrayList.clear();
        }


        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            LocalBroadcastManager.getInstance(GoogleMapService.this).sendBroadcast(new Intent(ACTION_SEND_GPS_AVAILABILITY_DATA).putExtra(LOCATION_AVAILABILITY_DATA, locationAvailability.isLocationAvailable()));
            super.onLocationAvailability(locationAvailability);
        }
    };

    @Override
    public void onDestroy() {
        releaseWakelock();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        Log.i("TEST", "Stopping service...");
        super.onDestroy();
    }


}
