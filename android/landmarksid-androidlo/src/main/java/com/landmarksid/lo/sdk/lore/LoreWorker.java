package com.landmarksid.lo.sdk.lore;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.LandmarksIDManager;
import com.landmarksid.lo.formats.DateTimeUtil;
import com.landmarksid.lo.listeners.EventListener;
import com.landmarksid.lo.listeners.LocationCallback;
import com.landmarksid.lo.permissions.PermissionUtil;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.Api;
import com.landmarksid.lo.sdk.EventLogger;
import com.landmarksid.lo.sdk.eventqueue.EventBatcher;

import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.Executor;

import io.sentry.Sentry;
import timber.log.Timber;

/**
 * Initializes Geofence and enques one LORE event to `EventBatcher`
 */
public class LoreWorker extends Worker {
    private static final String TAG = "landmarks.lore.worker";
    private EventListener log;

    public LoreWorker(Context context, @NonNull WorkerParameters params) {
        super(context, params);
        log = EventLogger.getInstance().getEventListener();
    }

    @NonNull
    @Override
    public Result doWork() {
        // check config from server, android sdk should be enabled
        if (!new PreferenceUtil(getApplicationContext()).getBooleanWithDefault(Const.Preferences.ANDROID_ENABLED, true))
            return Result.failure();

        //From Q, location permission changed, so check android version and check permission appropriately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !LandmarksIDManager.getInstance(getApplicationContext()).isInForeground()) {
            // Android 10 and app is in the background. Check for background location permissions
            if (!PermissionUtil.hasAllPermissions(getApplicationContext(), new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}))
                return Result.failure();
        }

        boolean hasMinPermissions = PermissionUtil.hasAllPermissions(getApplicationContext(), new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        if (!hasMinPermissions) {
            return Result.failure();
        }

        if (log != null) log.msg(TAG, "LORE Worker started successfully");


        SingleShotLocationProvider.requestSingleUpdate(getApplicationContext(), new LocationCallback() {
            @Override
            public void onLocationAvailable(Location location) {
                if(log != null) log.success(TAG, "Location obtained");
                sendLocation(location);
            }

            @Override
            public void onFailure() {
                Timber.d("No provider enabled");
                if(log != null) log.error(TAG, "No location provider enabled or usable");
            }
        });

        return Result.success();
    }

    private void sendLocation(Location location) {
        if(location == null) return;
        Timber.d("LORE Obtained: %s", location);

        try {
            if(log != null)
                log.onLore(TAG, DateTimeUtil.format(System.currentTimeMillis()), "", "ping");

            JSONObject requestJSON = Api.getJsonRequestLO(getApplicationContext(), DateTimeUtil.format(location.getTime()),
                    UUID.randomUUID().toString(), location.getLatitude(), location.getLongitude(), location.getSpeed(),
                    location.getAccuracy(), location.getAltitude(), Const.Values.EVENT_PERIODICAL);

            if (log != null) log.msg(TAG, "Adding LORE request to event queue: " + requestJSON);
            EventBatcher.getInstance(getApplicationContext()).addEventToQueue(requestJSON);

            // Reset geofences at current location
            LoreGeofence.getInstance(getApplicationContext()).initializeGeofence(location);

        } catch (Exception ex) {
            Sentry.captureException(ex);
        }
    }
}
