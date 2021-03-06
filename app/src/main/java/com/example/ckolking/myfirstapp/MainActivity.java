package com.example.ckolking.myfirstapp;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static java.lang.String.valueOf;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class MainActivity extends AppCompatActivity {

    /*
     Because each app uses Google Play services differently, it's up to you decide the appropriate place in your app to verify the Google Play services version. For example, if Google Play services is required for your app at all times, you might want to do it when your app first launches. On the other hand, if Google Play services is an optional part of your app, you can check the version only once the user navigates to that portion of your app.
     Another approach is to use the isGooglePlayServicesAvailable() method. You get a reference to the singleton object that provides this method using GoogleApiAvailability.getInstance(). You might call this method in the onResume() method of the main activity. If the result code is SUCCESS, then the Google Play services APK is up-to-date and you can continue to make a connection. If, however, the result code is SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED, or SERVICE_DISABLED, then the user needs to install an update. In this case, call the getErrorDialog() method and pass it the result error code. The method returns a Dialog you should show, which provides an appropriate message about the error and provides an action that takes the user to Google Play Store to install the update.
     */

    private LocationRequest mLocationRequest;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 7 * 1000; /* 7 sec */
    private int REQUEST_PERMISSIONS_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        This is called before initializing the map because the map needs permissions(the cause of the crash)
        */
        Log.d("CEK onCreate", valueOf(Build.VERSION.SDK_INT));
        Log.d("CEK onCreate", valueOf(Build.VERSION_CODES.M));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            checkPermission();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabEmail = (FloatingActionButton) findViewById(R.id.fabEmail);
        fabEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // ISSUE: the location update Toasts will not start if the end-user has not
        // already allowed this app to access device location information so:
        // upon first app use after APK installation, the location Toasts do not show, and
        // after allowing app access to device location info AND re-starting the app, then
        // the location update Toasts appear.
        // Solution:  move the 'startLocationUpdates' to callback from permissions request method.
        // Solution **part 2** : I still need this code here IF the user has ALREADY allowed this app
        // to access location, then start the updates, otherwise the request for permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }

        }

    public void checkPermission(){
        Log.d("CEK CheckPermission", "ACCESS_COARSE_LOCATION value is: " + valueOf(Manifest.permission.ACCESS_COARSE_LOCATION));
        Log.d("CEK CheckPermission", "ACCESS_FINE_LOCATION value is: " + valueOf(Manifest.permission.ACCESS_FINE_LOCATION));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] result){
        super.onRequestPermissionsResult(requestCode, permissions, result);

        if(requestCode == REQUEST_PERMISSIONS_CODE && result[0] == PackageManager.PERMISSION_GRANTED){
            //do things as usual init map or something else when location permission is granted
            Log.d("CEK onRequestPermissionsResult", "TO DO: start the auto-Toast location updates");
            startLocationUpdates();
        }
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    protected void onLocationChanged(Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d("CEK onLocationChanged", "Toast location update:" + msg);

        // You can now create a LatLng Object for use with maps
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        // TO DO: what shall I do with this new LatLng object??

    }

    protected void onButtonTap(View v) {
        Log.d("CEK onButtonTap", "Ouch!");
        Toast myToast = Toast.makeText(getApplicationContext(), "Ouch!", Toast.LENGTH_LONG);
        myToast.show();
    }

    public void onButtonLocation(View v) {
            /* */
        Log.d("CEK onButtonLocation", "start function");
        final double[] locationLat = {-1};
        FusedLocationProviderClient locationClient =
                getFusedLocationProviderClient(this);

        // Get the last known location
        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            onLocationChanged(location);
                            Log.d("CEK onSuccessListener-Location", valueOf( location.getLatitude()));
                            locationLat[0] = location.getLatitude();
                            Log.d("CEK onSuccessListener-Location", valueOf( locationLat[0] ));
                            //        Toast myToast = Toast.makeText(getApplicationContext(), "Here!", Toast.LENGTH_LONG);
                            Toast myToast = Toast.makeText(getApplicationContext(), "Here is: " + valueOf( locationLat[0] ), Toast.LENGTH_LONG);
                            myToast.show();
                        } else {
                            Log.d("CEK onSuccessListener-Location", "location is \'null\'");
                            Toast myToast = Toast.makeText(getApplicationContext(), "location is \'null\'", Toast.LENGTH_LONG);
                            myToast.show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("CEK onFailureListener-Location", "Error trying to get last GPS location");
                        e.printStackTrace();
                        Toast myToast = Toast.makeText(getApplicationContext(), "no location, error", Toast.LENGTH_LONG);
                        myToast.show();
                    }
                });

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
