package com.example.stepbuystep.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class TrackingService extends Service implements SensorEventListener {

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SensorManager sensorManager;
    private Sensor stepSensor;

    private boolean isTracking = false;
    private double totalDistance = 0.0;
    private int initialSteps = -1;
    private int currentSteps = 0;
    private Location lastLocation;

    public class LocalBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground immediately
        startForeground(1, createNotification("Ready to track"));
        return START_NOT_STICKY;
    }

    public void startTracking() {
        if (isTracking) return;

        isTracking = true;
        totalDistance = 0.0;
        initialSteps = -1;
        currentSteps = 0;
        lastLocation = null;

        startLocationUpdates();
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }

        updateNotification("Tracking started...");
    }

    public void stopTracking() {
        isTracking = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (stepSensor != null) {
            sensorManager.unregisterListener(this);
        }
        stopForeground(true);
    }

    public double getDistance() {
        return totalDistance;
    }

    public int getSteps() {
        return currentSteps;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(5)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (lastLocation != null) {
                        totalDistance += lastLocation.distanceTo(location) / 1000.0; // Convert to KM
                    }
                    lastLocation = location;
                    updateNotification(String.format("Dist: %.2f km | Steps: %d", totalDistance, currentSteps));
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            // Permission should be checked by Activity
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int total = (int) event.values[0];
            if (initialSteps == -1) {
                initialSteps = total;
            }
            currentSteps = total - initialSteps;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                "TRACKING_CHANNEL",
                "Tracking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, "TRACKING_CHANNEL")
                .setContentTitle("Activity Tracking")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(1, createNotification(text));
        }
    }
}
