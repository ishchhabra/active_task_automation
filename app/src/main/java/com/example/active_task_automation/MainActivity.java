package com.example.active_task_automation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final int BACKGROUND_LOCATION_PERMISSION_CODE = 43;
    private final int LOCATION_PERMISSION_CODE = 44;
    private final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 112;

    TextView latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = (TextView) findViewById(R.id.latitude);

        boolean hasWritePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasBackgroundLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Request external write permissions

        if (hasWritePermission && hasLocationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Need to ask BACKGROUND_LOCATION permission.
                if (hasBackgroundLocationPermission) {
                    startRecording();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, BACKGROUND_LOCATION_PERMISSION_CODE);
                }
            } else {
                startRecording();
            }
        } else {
            if (!hasWritePermission) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                }, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, LOCATION_PERMISSION_CODE);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, LOCATION_PERMISSION_CODE);
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Need to ask BACKGROUND_LOCATION permission.
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, BACKGROUND_LOCATION_PERMISSION_CODE);
                } else {
                    startRecording();
                }
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        // GPS information
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        waitForLocationPermission(locationManager);
    }

    private void waitForLocationPermission(LocationManager locationManager) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startRecordingLocationInfo(locationManager);
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    waitForLocationPermission(locationManager);
                }
            }, 1000);
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecordingLocationInfo(LocationManager locationManager) {
        // Start foreground service.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context applicationContext = getApplicationContext();
            applicationContext.startForegroundService(new Intent(applicationContext, BackgroundService.class));
            finish();
        }
    }



}