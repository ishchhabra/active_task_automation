package com.example.active_task_automation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private File gps_fd;
    private FileOutputStream gps_fos;

    private File dnd_fd;
    private FileOutputStream dnd_fos;

    private File accelerometer_fd;
    private FileOutputStream accelerometer_fos;

    private File barometer_fd;
    private FileOutputStream barometer_fos;

    private final int LOCATION_PERMISSION_CODE = 44;
    private final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean allPermissionsPresent = true;

        // Request external write permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
            allPermissionsPresent = false;
        }

        // Request GPS permissions.
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (allPermissionsPresent) { // To avoid requesting multiple permissions at once.
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_CODE);

                allPermissionsPresent = false;
            }
        }

        if (allPermissionsPresent) {
            startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean writePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!writePermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
            return;
        }

        boolean gpsPermissions = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!gpsPermissions) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_CODE);
            return;
        }

        startRecording();
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        // Open all files.
        try {
            gps_fd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/gps_data.csv");
//                    new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "gps_data.csv"); // Use this for old android versions.
            if (!gps_fd.exists()) {
                gps_fd.createNewFile();
            }
            gps_fos = new FileOutputStream(gps_fd);

            dnd_fd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/dnd_data.csv");
            if (!dnd_fd.exists()) {
                dnd_fd.createNewFile();
            }
            dnd_fos = new FileOutputStream(dnd_fd);

            accelerometer_fd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/accelerometer_data.csv");
            if (!accelerometer_fd.exists()) {
                accelerometer_fd.createNewFile();
            }
            accelerometer_fos = new FileOutputStream(accelerometer_fd);

            barometer_fd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/barometer_data.csv");
            if (!barometer_fd.exists()) {
                barometer_fd.createNewFile();
            }
            barometer_fos = new FileOutputStream(barometer_fd);

        } catch (IOException e) {
            e.printStackTrace();
        }

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer information
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(new MyAccelerometerListener(), accelerometerSensor, SensorManager.SENSOR_ACCELEROMETER);

        // Pressure information
        Sensor barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(new MyBarometerListener(), barometerSensor, SensorManager.SENSOR_ACCELEROMETER);

        // GPS information
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

        // DND information
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                try {
                    dnd_fos.write(
                            String.format("%d;%d\n",
                                    System.currentTimeMillis(),
                                    getDNDStatus()).getBytes()
                    );
                } catch (IOException | Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 1000);
    }

    private int getDNDStatus() throws Settings.SettingNotFoundException {
        return Settings.Global.getInt(getContentResolver(), "zen_mode");
    }

    private class MyLocationListener implements LocationListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(@NonNull Location location) {
            try {
                Log.d("GPS", String.format("%d;%f;%f\n",
                        location.getTime(),
                        location.getLatitude(),
                        location.getLongitude()));

                gps_fos.write(
                        String.format("%d;%f;%f\n",
                                location.getTime(),
                                location.getLatitude(),
                                location.getLongitude()).getBytes()
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) { }

        @Override
        public void onProviderDisabled(@NonNull String provider) { }
    }

    private class MyAccelerometerListener implements SensorEventListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            try {
                accelerometer_fos.write(
                        String.format("%d;%f;%f;%f\n",
                                sensorEvent.timestamp,
                                sensorEvent.values[0],
                                sensorEvent.values[1],
                                sensorEvent.values[2]).getBytes()
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    private class MyBarometerListener implements SensorEventListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            try {
                barometer_fos.write(
                        String.format("%d;%f\n",
                                sensorEvent.timestamp,
                                sensorEvent.values[0]).getBytes()
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}