package com.example.active_task_automation;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundService extends Service {
    private final String NOTIFICATION_CHANNEL_ID = "location";
    private final String ACTION_STOP_SERVICE = "STOP_BACKGROUND_SERVICE";

    private File gps_fd, dnd_fd, accelerometer_fd, barometer_fd;
    private FileOutputStream gps_fos, dnd_fos, accelerometer_fos, barometer_fos;

    public BackgroundService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_launcher_background);

        Intent stopSelf = new Intent(this, BackgroundService.class);
        stopSelf.setAction(ACTION_STOP_SERVICE);

        PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT);

        builder.addAction(R.drawable.ic_launcher_background, "STOP", pStopSelf);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(NOTIFICATION_CHANNEL_ID);
            notificationChannel.setSound(null, null);
            notificationManager.createNotificationChannel(notificationChannel);
            startForeground(1, builder.build());
        }

        startRecording();
    }

    @Override
    public void onDestroy() {
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

    @SuppressLint("MissingPermission")
    public void startRecording() {
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
        sensorManager.registerListener(new SensorEventListener() {
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
            public void onAccuracyChanged(Sensor sensor, int i) {}
        }, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Pressure information
        Sensor barometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(new SensorEventListener() {
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
            public void onAccuracyChanged(Sensor sensor, int i) {}
        }, barometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

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

        // GPS information
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();

                Log.d("GPS", "" + location.getLatitude());
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

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private int getDNDStatus() throws Settings.SettingNotFoundException {
        return Settings.Global.getInt(getContentResolver(), "zen_mode");
    }
}