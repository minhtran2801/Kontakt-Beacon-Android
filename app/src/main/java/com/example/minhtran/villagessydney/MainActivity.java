package com.example.minhtran.villagessydney;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private TextView nameTextView;
    private ImageView profileImgView;
    private WebView myWebView;
    private myWebViewClient my_WebviewClient;
    private Intent serviceIntent;
    private MenuItem update;
    private MenuItem logoff;
    private MenuItem login;
    private MenuItem register;
    private boolean isBeaconLost = false;
    private static Boolean foregroundRunning = false;
    private final String API_KEY = "KnNzaNoPnLqBedgVSVnEbcpfXljhImAl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebkitCookieManagerProxy myCookieManager = new WebkitCookieManagerProxy(null, java.net.CookiePolicy.ACCEPT_ALL);
        java.net.CookieHandler.setDefault(myCookieManager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        KontaktSDK.initialize(API_KEY);
        serviceIntent = new Intent(getApplicationContext(), ForegroundService.class);
        serviceIntent = ForegroundService.createIntent(this);

        myWebView = (WebView) findViewById(R.id.webview);
        nameTextView = (TextView)findViewById(R.id.nameTextView);
        profileImgView = (ImageView)findViewById(R.id.profileImg);
        myWebView.getSettings().setJavaScriptEnabled(true);
        my_WebviewClient = new myWebViewClient(this);
        myWebView.setWebViewClient(my_WebviewClient);
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= 19) {
            myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            myWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        updateInterface();
        Intent intent = getIntent();
        Boolean isLoggedIn = getPreferenceIsLoggedIn();
        String openDialog = intent.getStringExtra("openFragment");
        if (openDialog != null && isLoggedIn) {
            if (getNoOfNoti() < 2 && !isBeaconLost) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("Notification");
                dialogBuilder.setMessage("Hey, we found you a coupon. Would you like to know more in 45 minutes?");
                dialogBuilder.setCancelable(true);

                dialogBuilder.setPositiveButton("Yes, please", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (!isBeaconLost) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    if (validNoOfNoti()) {
                                        showNotification("Hey there, we found you another discount. Enjoy!");
                                    }

                                }
                            }, TimeUnit.MINUTES.toMillis(45));


                        }
                    }
                });

                dialogBuilder.setNegativeButton("No, thank you", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (validNoOfNoti()) {
                            updateNoOfNoti();
                        }
                    }
                });
                dialogBuilder.show();
                resetNoOfNoti();
            }
        }

        if (!foregroundRunning && isLoggedIn) {
            startService(serviceIntent);
            // findCoordinate();
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        update = menu.findItem(R.id.action_update);
        logoff = menu.findItem(R.id.action_logout);
        login = menu.findItem(R.id.action_login);
        register = menu.findItem(R.id.action_register);

        Boolean isLoggedIn = getPreferenceIsLoggedIn();
        if (isLoggedIn) {
            update.setVisible(true);
            logoff.setVisible(true);
            login.setVisible(false);
            register.setVisible(false);
        } else {
            update.setVisible(false);
            logoff.setVisible(false);
            login.setVisible(true);
            register.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_home:
                myWebView.loadUrl("https://www.villages.sydney/");
                break;
            case R.id.action_login:
                myWebView.loadUrl("https://www.villages.sydney/app-login?hide_header=1");
                break;
            case R.id.action_register:
                myWebView.loadUrl("https://www.villages.sydney/member/login?hide_header=1");
                break;
            case R.id.action_update:
                myWebView.loadUrl("https://www.villages.sydney/profile/edit");
                break;
            case R.id.action_logout:
                myWebView.loadUrl("https://www.villages.sydney/logout");
                logOutPreference();
                stopForeGroundScanning();
                profileImgView.setVisibility(View.GONE);
                nameTextView.setText("");
                update.setVisible(false);
                logoff.setVisible(false);
                my_WebviewClient.setLoggedIn(false);
                break;
            case R.id.action_search:
                myWebView.loadUrl("https://www.villages.sydney/search");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void stopForeGroundScanning() {
        stopService(serviceIntent);
        foregroundRunning = false;
    }
    private void updateInterface() {
        if (!getPreferenceIsLoggedIn()) {
            my_WebviewClient.setLoggedIn(false);
            profileImgView.setVisibility(View.GONE);
            myWebView.loadUrl("https://www.villages.sydney/app-login?hide_header=1");
            // myWebView.loadUrl("https://www.villages.sydney/member/login");
        } else {
            my_WebviewClient.setLoggedIn(true);
            profileImgView.setVisibility(View.VISIBLE);
            if (getImgURLPref() == null) {
                profileImgView.setImageResource(R.drawable.img);
            } else {
                // Load from URL
                //String url = getImgURLPref();
                //Picasso.get().load(url).centerCrop().into(profileImgView);
            }
            profileImgView.setImageDrawable(getResources().getDrawable(R.drawable.yellow_background));
            nameTextView.setText(getNamePref());
            myWebView.loadUrl("https://www.villages.sydney");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ForegroundService.ACTION_DEVICE_DISCOVERED);
        registerReceiver(scanningBroadcastReceiver, intentFilter);

        IntentFilter urlIntentFiler = new IntentFilter(myWebViewClient.ACTION_TRANSFER_URL);
        registerReceiver(reloadURL, urlIntentFiler);

        IntentFilter beaconStatusFilter = new IntentFilter(ForegroundService.ACTION_TRANSFER_BEACON_STATUS);
        registerReceiver(beaconStatusReceiver, beaconStatusFilter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(scanningBroadcastReceiver);
        unregisterReceiver(reloadURL);
        unregisterReceiver(beaconStatusReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i("update", "on Destroy Main act");
        stopService(serviceIntent);

        super.onDestroy();
    }

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    private final BroadcastReceiver scanningBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Device discovered!
            int devicesCount = intent.getIntExtra(ForegroundService.EXTRA_DEVICES_COUNT, 0);
            boolean isFirstBeacon = intent.getBooleanExtra(ForegroundService.IS_FIRST_FOUND, true);
            boolean isLoggedIn = getPreferenceIsLoggedIn();
            foregroundRunning = true;

            if (isFirstBeacon && validNoOfNoti()) {
              if (isLoggedIn) {
                  showNotification("Welcome, we found a beacon");
              }
            }
            RemoteBluetoothDevice device = intent.getParcelableExtra(ForegroundService.EXTRA_DEVICE);
            Log.i("On discovered", "Total discovered devices: " + devicesCount + "\n\nLast scanned device:\n" + device.toString());
        }
    };

    private final BroadcastReceiver beaconStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Device discovered!
            boolean isOutOfRange = intent.getBooleanExtra(ForegroundService.IS_BEACON_LOST, false);
            if (isOutOfRange) {
                isBeaconLost = true;
            } else {
                isBeaconLost = false;
            }
            foregroundRunning = true;
        }
    };

    private final BroadcastReceiver reloadURL = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          updateInterface();
            if (!foregroundRunning) {
                startService(serviceIntent);
                // findCoordinate();
            }
        }
    };


    private void findCoordinate() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.i("TEST", "finding coordinate");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i("TEST", "checking permission");
            return;
        }
        Log.i("TEST", "permission granted");
        Location l = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (l != null) {
            Log.i("TEST", "getlastknown" + l.getLongitude() + l.getLatitude());
            String address = getAddressFromLocation(l.getLongitude(), l.getLatitude());
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 3, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, this);


    }

    private String getAddressFromLocation(double longitude, double latitude) {
        Log.i("Test", "test");
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        StringBuilder strAddress = new StringBuilder();
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                Address fetchedAddress = addresses.get(0);
                for (int i = 0; i <= fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append(" ");
                    strAddress.append(fetchedAddress.getExtras());
                }

                Log.i("address", strAddress.toString());

            } else {
                Log.i("address", "searching for addressses");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.i("address", "Could not get address..!");
        }

        return strAddress.toString();
    }

    @Override
    public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        Log.i("Coordinate", Double.toString(longitude) + Double.toString(latitude));
        String address = getAddressFromLocation(longitude, latitude);
        Log.i("Address", address);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i("Warning", "GPS OFF");
    }

    private void logOutPreference() {
        SharedPreferences sharedPref = this.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("EMAIL", "");
        editor.putString("FNAME", "");
        editor.putString("LNAME", "");
        editor.putString("IMGURL", "");
        editor.putBoolean("isLoggedIn", false);
        editor.commit();
    }

    private void resetNoOfNoti() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this,ResetNoOfNoti.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        Log.i("RESET", "ALL SET");
    }
    private boolean validNoOfNoti() {
        if (getNoOfNoti() >= 2) {
            return false;
        }
        return true;
    }


    private String getImgURLPref() {
        SharedPreferences sharedPref = this.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String url = sharedPref.getString("IMGURL","");
        if (url != "") {
            return url;
        }
        return null;
    }
    private String getNamePref() {
        SharedPreferences sharedPref = this.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String fname = sharedPref.getString("FNAME","");
        String lname = sharedPref.getString("LNAME", "");
        if (fname != "" && lname != ""){
            String fullname =  fname + " " + lname;
            return fullname;
        }
        return null;
    }

    private boolean getPreferenceIsLoggedIn() {
        SharedPreferences sharedPref = this.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPref.getBoolean("isLoggedIn", false);

        Log.i("check isLoggedIn", "" + isLoggedIn);
        return isLoggedIn;
    }


    private int getNoOfNoti() {
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.NotificationSetting), Context.MODE_PRIVATE);
        int defaultValue = getResources().getInteger(R.integer.default_no_of_noti);
        int noOfNoti = sharedPref.getInt(getString(R.string.noOfNoti), defaultValue);

        Log.i("check NOTI ", "No of noti " + noOfNoti);
        return noOfNoti;
    }


    private void updateNoOfNoti() {
        int noOfNoti = getNoOfNoti();
        noOfNoti++;
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.NotificationSetting), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.noOfNoti), noOfNoti);
        editor.commit();
        Log.i("INCREASE NOTI ", "No of noti " + noOfNoti);
    }

    private void showNotification(final String message) {
        updateNoOfNoti();
        Intent resultIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        resultIntent.putExtra("openFragment", "Opening dialog box");
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, resultIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ID")
                .setSmallIcon(R.drawable.moble_logo)
                .setContentTitle("Villages Sydney")
                .setContentText(message)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(4, builder.build());
    }

}
