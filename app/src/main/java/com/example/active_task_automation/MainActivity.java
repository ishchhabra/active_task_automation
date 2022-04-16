package com.example.active_task_automation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import org.w3c.dom.Text;

//import com.google.android.gms.location.FusedLocationProviderClient;


public class MainActivity extends AppCompatActivity {
    //    LocationManager locationManager;
    //    LocationListener locationListener;
    private LocationManager locationManager;
    private TextView longitudeTextView;
    private TextView latitudeTextView;
    int PERMISSION = 44;
    private TextView DNDTextView;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeTextView = (TextView) findViewById(R.id.latitude);
        longitudeTextView = (TextView) findViewById(R.id.longitude);
        DNDTextView = (TextView) findViewById(R.id.dndstatus);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        if (checkPermissions()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } else {
            requestPermissions();
        }

        int ret = 0;
        try {
            ret = getDNDStatus();
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        DNDTextView.setText("" + ret);
    }

    public int getDNDStatus() throws Settings.SettingNotFoundException {
        return Settings.Global.getInt(getContentResolver(), "zen_mode");
    }

    private boolean checkPermissions() {
        boolean check1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean check2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return check1 && check2;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        }, PERMISSION);
    }

    class MyLocationListener implements LocationListener{


        @Override
        public void onLocationChanged(@NonNull Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            latitudeTextView.setText("" + latitude);
            longitudeTextView.setText("" + longitude);
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
}