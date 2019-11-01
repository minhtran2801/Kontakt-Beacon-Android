package com.example.minhtran.villagessydney;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ForceScanConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.rssi.RssiCalculators;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ForegroundService extends Service {

    public static final String TAG = ForegroundService.class.getSimpleName();

    public static final String ACTION_DEVICE_DISCOVERED = "DEVICE_DISCOVERED_ACTION";
    public static final String ACTION_TRANSFER_BEACON_STATUS = "ACTION_TRANSFER_BEACON_STATUS";

    public static final String IS_BEACON_LOST = "IS_BEACON_LOST";
    public static final String IS_FIRST_FOUND = "IS_FIRSt_FOUND";
    public static final String EXTRA_DEVICE = "DeviceExtra";
    public static final String EXTRA_DEVICES_COUNT = "DevicesCountExtra";

    private static final String STOP_SERVICE_ACTION = "STOP_SERVICE_ACTION";

    private static final String NOTIFICATION_CHANEL_NAME = "Beacon scanning";
    private static final String NOTIFICATION_CHANEL_ID = "scanning_service_channel_id";

    private ProximityManager proximityManager;
    private boolean isRunning; // Flag indicating if service is already running.
    private int devicesCount; // Total discovered devices count
    private static int lastMinor = 0;

    public static Intent createIntent(final Context context) {
        return new Intent(context, ForegroundService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupProximityManager();
        isRunning = false;
    }

    private void setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this);

        proximityManager.configuration()
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.RANGING)
                .activityCheckConfiguration(ActivityCheckConfiguration.DISABLED)
                .forceScanConfiguration(ForceScanConfiguration.DISABLED)
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(1))
                .rssiCalculator(RssiCalculators.DEFAULT);

        // Set up iBeacon listener
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (STOP_SERVICE_ACTION.equals(intent.getAction())) {
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        // Check if service is already active
        if (isRunning) {
            Toast.makeText(this, "Service is already running.", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        }

        startInForeground();
        startScanning();
        isRunning = true;
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (proximityManager != null) {
            proximityManager.disconnect();
            proximityManager = null;
        }

        NotificationManager notifManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancelAll();

        Toast.makeText(this, "Scanning service stopped.", Toast.LENGTH_SHORT).show();
        Log.i("update", "Scan stop foreground");
        super.onDestroy();
    }

    private void startInForeground() {
        // Create notification intent
        final Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0
        );

        // Create stop intent with action
        final Intent intent = ForegroundService.createIntent(this);
        intent.setAction(STOP_SERVICE_ACTION);
        final PendingIntent stopIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // Build notification
        final NotificationCompat.Action action = new NotificationCompat.Action(0, "Stop", stopIntent);
        final Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANEL_ID)
                .setContentTitle("Villages Sydney")
                .setContentText("Actively scanning iBeacons")
                .addAction(action)
                .setSmallIcon(R.drawable.moble_logo)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();


        // Start foreground service
        startForeground(1, notification);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        final NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                NOTIFICATION_CHANEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
                devicesCount = 0;
                Toast.makeText(ForegroundService.this, "Scanning service started.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                onDeviceDiscovered(ibeacon);
                transferBeaconStatus(false);
                Log.i(TAG, "onIBeaconDiscovered: " + ibeacon.toString());
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {

                List<IBeaconDevice> sortList = new ArrayList<>(iBeacons);

                Collections.sort(sortList, new SortBeaconDistance());

                for (IBeaconDevice iBeacon : sortList) {
                    Log.i("Update sorted 23", iBeacon.getMinor() + " " + String.format("%.5f", iBeacon.getDistance()));
                }


                String URL = "";
                int currentClosestMinor = sortList.get(0).getMinor();
                if (lastMinor != currentClosestMinor) {
                    lastMinor = currentClosestMinor;
                    if (currentClosestMinor == 55555) {
                        Toast.makeText(getApplicationContext(), "Current closest: beacon 1 ", Toast.LENGTH_LONG).show();
                    } else if (currentClosestMinor == 59590) {
                        Toast.makeText(getApplicationContext(), "Current closest: beacon 2", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.i("update", "same minor");
                }

            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                super.onIBeaconLost(ibeacon, region);
                transferBeaconStatus(true);
                Log.e(TAG, "onIBeaconLost: " + ibeacon.toString());
            }
        };
    }


    private void onDeviceDiscovered(final RemoteBluetoothDevice device) {
        devicesCount++;
        //Send a broadcast with discovered device
        Intent intent = new Intent();
        intent.setAction(ACTION_DEVICE_DISCOVERED);
        intent.putExtra(EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_DEVICES_COUNT, devicesCount);
        if (devicesCount == 1) {
            intent.putExtra(IS_FIRST_FOUND, true);
        } else {
            intent.putExtra(IS_FIRST_FOUND, false);
        }
        sendBroadcast(intent);
    }

    private void transferBeaconStatus(Boolean isLost) {
        Intent intent = new Intent();
        intent.setAction(ACTION_TRANSFER_BEACON_STATUS);
        intent.putExtra(IS_BEACON_LOST, true);
    }
}

class SortBeaconDistance implements Comparator<IBeaconDevice> {

    @Override
    public int compare(IBeaconDevice o1, IBeaconDevice o2) {
        int i = Double.compare(o1.getDistance(), o2.getDistance());
        return i;
    }
}

