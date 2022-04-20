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
import android.provider.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private File gps_fd;
    private FileOutputStream gps_fos;

    private File dnd_fd;
    private FileOutputStream dnd_fos;

    private File accelerometer_fd;
    private FileOutputStream accelerometer_fos;

    private File barometer_fd;
    private FileOutputStream barometer_fos;

    private int LOCATION_PERMISSION_CODE = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Open all files.
        try {
            gps_fd = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "gps_data.csv");
            gps_fos = new FileOutputStream(gps_fd);

            dnd_fd = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "dnd_data.csv");
            dnd_fos = new FileOutputStream(dnd_fd);

            accelerometer_fd = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "accelerometer_data.csv");
            accelerometer_fos = new FileOutputStream(accelerometer_fd);

            barometer_fd = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "barometer_data.csv");
            barometer_fos = new FileOutputStream(barometer_fd);

        } catch (FileNotFoundException e) {
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
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_CODE);
        }

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
        }, 0, 1000);
    }

    private int getDNDStatus() throws Settings.SettingNotFoundException {
        return Settings.Global.getInt(getContentResolver(), "zen_mode");
    }

    private class MyLocationListener implements LocationListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(@NonNull Location location) {
            try {
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
        public void onProviderEnabled(@NonNull String provider) {
            LocationListener.super.onProviderEnabled(provider);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            LocationListener.super.onProviderDisabled(provider);
        }
    }

    private class MyAccelerometerListener implements SensorEventListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            try {
                accelerometer_fos.write(
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

    private class MyBarometerListener implements SensorEventListener {
        @SuppressLint("DefaultLocale")
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            try {
                barometer_fos.write(
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
}