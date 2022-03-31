package com.example.active_task_automation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

//import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;

public class MainActivity extends AppCompatActivity {
    //    LocationManager locationManager;
//    LocationListener locationListener;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    200);
        }
        Log.e("[GPS]", fusedLocationClient.toString());
        fusedLocationClient.requestLocationUpdates(new LocationRequest(), new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if (locationResult == null) {
                    Log.e("[GPS]", "SSSSSSSSSSS");
                }

                for (Location location : locationResult.getLocations()) {
                    Log.e("[GPS]", String.valueOf(location.getTime()));
                }
            }
        }, Looper.getMainLooper());


//        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        this.locationListener = new LocationListener();
//
//        Log.e("[GPS]", locationListener.toString());
//        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
}

//
//class LocationListener implements android.location.LocationListener {
//    @Override
//    public void onLocationChanged(Location location) {
//        if (location != null) {
//            double latitude = location.getLatitude();
//            double longitude = location.getLongitude();
//
//            Log.e("[GPS]",  latitude + " " +  longitude);
//        }
//    }
//
//
//}