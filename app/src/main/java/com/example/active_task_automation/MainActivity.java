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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private FusedLocationProviderClient fusedLocationProviderClient;

    private File gps_fd;
    private FileOutputStream gps_fos;

    private File dnd_fd;
    private FileOutputStream dnd_fos;

    private File accelerometer_fd;
    private FileOutputStream accelerometer_fos;

    private File barometer_fd;
    private FileOutputStream barometer_fos;

    private final int BACKGROUND_LOCATION_PERMISSION_CODE = 43;
    private final int LOCATION_PERMISSION_CODE = 44;
    private final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 112;

    TextView latitude;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = (TextView) findViewById(R.id.latitude);

        boolean hasWritePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasBackgroundLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Request external write permissions
        if (hasWritePermission && hasLocationPermission && hasBackgroundLocationPermission) {
            startRecording();
        } else {
            if (!hasWritePermission) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                }, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
            } else {
                if (!hasLocationPermission) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, LOCATION_PERMISSION_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, BACKGROUND_LOCATION_PERMISSION_CODE);
                }
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            gps_fos.close();
            dnd_fos.close();
            accelerometer_fos.close();
            barometer_fos.close();
        } catch (IOException e) {
            e.printStackTrace();
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
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                }, BACKGROUND_LOCATION_PERMISSION_CODE);
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        // Open all files.
        try {
            File root_directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            gps_fd = new File(root_directory, "gps_data.csv");
            if (!gps_fd.exists()) {
                gps_fd.createNewFile();
            }
            gps_fos = new FileOutputStream(gps_fd, true);

            dnd_fd = new File(root_directory, "dnd_data.csv");
            if (!dnd_fd.exists()) {
                dnd_fd.createNewFile();
            }
            dnd_fos = new FileOutputStream(dnd_fd, true);

            accelerometer_fd = new File(root_directory, "accelerometer_data.csv");
            if (!accelerometer_fd.exists()) {
                accelerometer_fd.createNewFile();
            }
            accelerometer_fos = new FileOutputStream(accelerometer_fd, true);

            barometer_fd = new File(root_directory, "barometer_data.csv");
            if (!barometer_fd.exists()) {
                barometer_fd.createNewFile();
            }
            barometer_fos = new FileOutputStream(barometer_fd, true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer information
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(new MyAccelerometerListener(), accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Pressure information
        Sensor barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(new MyBarometerListener(), barometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // GPS information
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        waitForLocationPermission(locationManager);

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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

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
        }, Looper.getMainLooper());
    }

    private int getDNDStatus() throws Settings.SettingNotFoundException {
        return Settings.Global.getInt(getContentResolver(), "zen_mode");
    }

//    private class MyLocationListener implements LocationListener {
//        @SuppressLint("DefaultLocale")
//        @Override
//        public void onLocationChanged(@NonNull Location location) {
//            try {
//                Log.d("GPS", String.format("%d;%f;%f\n",
//                        location.getTime(),
//                        location.getLatitude(),
//                        location.getLongitude()));
//
//                latitude.setText("" + location.getLatitude());
//
//                gps_fos.write(
//                        String.format("%d;%f;%f\n",
//                                location.getTime(),
//                                location.getLatitude(),
//                                location.getLongitude()).getBytes()
//                );
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

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