package com.landmarksid.lo.sdk.lore;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentServiceImpl;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.formats.DateTimeUtil;
import com.landmarksid.lo.listeners.EventListener;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.Api;
import com.landmarksid.lo.sdk.EventLogger;
import com.landmarksid.lo.sdk.eventqueue.EventBatcher;

import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import io.sentry.Sentry;
import timber.log.Timber;

public class LoreGeofenceWorker extends Worker {
    private static final String TAG = "landmarks.lore.geo.job";

    private static final int JOB_ID = 573;
    //THIS MIUST BE INITIALI AS PRAMETERS IN INTENT
    private static Intent mIntent;

    private EventListener log;

    public LoreGeofenceWorker(Context context, @NonNull WorkerParameters params) {
        super(context, params);
        log = EventLogger.getInstance().getEventListener();
    }

    public static void prepareWork(Context context, Intent intent) {
        mIntent = intent;
    }

    @NonNull @Override
    public Result doWork() {
        Timber.d("Geofence LORE JobIntentService triggered");

        log = EventLogger.getInstance().getEventListener();

        if(!new PreferenceUtil(getApplicationContext()).getBooleanWithDefault(Const.Preferences.ANDROID_ENABLED, true))
            return Result.failure();

        if(log != null) log.msg(TAG, "LORE Geofence triggered");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(mIntent);
        if(geofencingEvent.hasError()) {
            Timber.d("Geofence error: %s", geofencingEvent.getErrorCode());
            return Result.failure();
        }

        if(geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> geofences = geofencingEvent.getTriggeringGeofences();

            Timber.d("EXIT EVENTS: " + geofences.size() + ": " + geofences.get(0));

            Location location = geofencingEvent.getTriggeringLocation();
            if(location != null) {
                sendLore(location);
                reinitializeGeofence(location);
            }
        }
        return Result.success();
    }

    private void sendLore(Location location) {
        try {
            if(log != null)
                log.onLore(TAG, DateTimeUtil.format(System.currentTimeMillis()), "", "geofence");

            JSONObject request = Api.getJsonRequestLO(getApplicationContext(), DateTimeUtil.format(location.getTime()),
                    UUID.randomUUID().toString(), location.getLatitude(), location.getLongitude(), location.getSpeed(),
                    location.getAccuracy(), location.getAltitude(), Const.Values.EVENT_GEOFENCE);

            if (log != null) log.msg(TAG, "Adding LORE request to event queue: " + request);
            EventBatcher.getInstance(getApplicationContext()).addEventToQueue(request);

        } catch (Exception ex) {
            Sentry.captureException(ex);
        }
    }

    private void reinitializeGeofence(Location location) {
        LoreGeofence loreGeofence = LoreGeofence.getInstance(getApplicationContext());
        loreGeofence.initializeGeofence(location);
    }
}
