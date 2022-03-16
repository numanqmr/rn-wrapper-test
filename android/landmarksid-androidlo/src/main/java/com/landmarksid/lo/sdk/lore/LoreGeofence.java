package com.landmarksid.lo.sdk.lore;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.preferences.PreferenceUtil;

import io.sentry.Sentry;
import timber.log.Timber;

public class LoreGeofence {
    private static final String TAG = "landmarks.lore.geofence";

    private static final String GEOFENCE_DEFAULT_ID = "landmarks-default";
    private static final long GEOFENCE_DEFAULT_RADIUS = 50; // Meters
    private static final long DEFAULT_SPEED_KPH = Integer.MAX_VALUE-1;

    private GeofencingClient geofencingClient;

    private Geofence geofence;
    private PendingIntent geofencePendingIntent;

    private PreferenceUtil prefUtil;

    private static LoreGeofence instance;

    private LoreGeofence() {
        if(instance != null)
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
    }

    public static LoreGeofence getInstance(Context context) {
        if(instance == null) {
            synchronized (LoreGeofence.class) {
                if(instance == null) {
                    instance = new LoreGeofence();

                    if(instance.geofencingClient == null)
                        instance.geofencingClient = LocationServices.getGeofencingClient(context);

                    if(instance.prefUtil == null)
                        instance.prefUtil = new PreferenceUtil(context);

                    if(instance.geofencePendingIntent == null)
                        instance.geofencePendingIntent = instance.getGeofencePendingIntent(context);
                }
            }
        }

        return instance;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)
                .addOnCanceledListener(new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        Timber.d("Geofences cancelled");
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Timber.d("Geofences completed: %s", task.toString());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timber.d("Geofences successfully added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.d("Geofences failed: %s", e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                });
    }

    public void addGeofence(Location location, float radius) {
        try {
            this.geofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_DEFAULT_ID)
                    .setCircularRegion(location.getLatitude(), location.getLongitude(), radius == 0.0 ? GEOFENCE_DEFAULT_RADIUS : radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            Timber.d("Geofence created at: %s,%s with radius: %s", location.getLatitude(), location.getLongitude(), radius);

        } catch (Exception ex) {
            ex.printStackTrace();
            Sentry.captureException(ex);
        }
    }

    void initializeGeofence(Location location) {
        try {
            Timber.d("Reinitializing geofence at %s", location);
            remove();

            double lastLat = prefUtil.getDouble(Const.Preferences.LAST_LOC_LAT);
            double lastLong = prefUtil.getDouble(Const.Preferences.LAST_LOC_LONG);
            long lastTime = prefUtil.getLong(Const.Preferences.LAST_LOC_TIME);

            double speed = 0.0;
            if (location.hasSpeed() && location.getSpeed() > 0.0) {
                speed = location.getSpeed();

            } else if (lastLat > 0.0 && lastLong > 0.0 && lastTime > 0L) {
                double dist = LoreGeofence.distance(lastLat, location.getLatitude(), lastLong, location.getLongitude());
                long time = (location.getTime() - lastTime) / 1000L;

                speed = dist / time;
                Timber.d("Distance: %s Time: %s Speed: %s", dist, time, speed);
            }

            long radius = getDynamicDistanceInterval(speed);
            Timber.d("Dynamic Geofence Radius: %s", radius);

            addGeofence(location, radius);
            start();

            prefUtil.put(Const.Preferences.LAST_LOC_LAT, location.getLatitude());
            prefUtil.put(Const.Preferences.LAST_LOC_LONG, location.getLongitude());
            prefUtil.put(Const.Preferences.LAST_LOC_TIME, location.getTime());

        } catch (Exception ex) {
            Sentry.captureException(ex);
        }
    }

    /**
     * removes all geofences
     */
    public void remove() {
        if(geofencingClient != null && geofencePendingIntent != null) {
            geofencingClient.removeGeofences(geofencePendingIntent);

            Timber.d("All geofences removed");
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofence(geofence);

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(Context context) {
        if(geofencePendingIntent != null)
            return geofencePendingIntent;

        Intent intent = new Intent(context, LoreGeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofencePendingIntent;
    }

    /**
     * Calculates the optimum radius based on given speed for next geofence to be setup
     * @param speed
     * @return
     */
    public long getDynamicDistanceInterval(double speed) {
        long minSpeedKph = prefUtil.getLongWithDefault(Const.Preferences.MIN_SPEED_KPH, DEFAULT_SPEED_KPH);
        long maxSpeedKph = prefUtil.getLongWithDefault(Const.Preferences.MAX_SPEED_KPH, DEFAULT_SPEED_KPH);
        long distanceInterval = prefUtil.getLongWithDefault(Const.Preferences.DISTANCE_INTERVAL_METERS, GEOFENCE_DEFAULT_RADIUS);

        double deviceSpeedKph = speed * 3.6; // m/s to km/h

        if(deviceSpeedKph < minSpeedKph)
            return distanceInterval;

        else if(speed > maxSpeedKph)
            return 3000;
        else
            return distanceInterval * (long) Math.pow(deviceSpeedKph/minSpeedKph, 2);
    }

    private static double distance(double lat1, double lat2, double lon1, double lon2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // meters
    }
}
