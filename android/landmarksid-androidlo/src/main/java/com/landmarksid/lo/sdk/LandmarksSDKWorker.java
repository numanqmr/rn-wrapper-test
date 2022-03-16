package com.landmarksid.lo.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.LandmarksIDManager;
import com.landmarksid.lo.formats.DateTimeUtil;
import com.landmarksid.lo.listeners.AppStateListener;
import com.landmarksid.lo.listeners.ConfigListener;
import com.landmarksid.lo.listeners.DataCollectionCallbackListener;
import com.landmarksid.lo.listeners.EventListener;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.analytics.AnalyticsUtil;
import com.landmarksid.lo.sdk.eventqueue.EventBatcher;
import com.landmarksid.lo.sdk.lore.LoreGeofence;
import com.landmarksid.lo.sdk.lore.LoreWorker;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.sentry.Sentry;
import timber.log.Timber;

/**
 * TODO: see if we can make it private
 */
public class LandmarksSDKWorker extends Worker implements AppStateListener {
    private static final String TAG = "landmarks.service.sdk";

    private static final long DEFAULT_TIME_INTERVAL = 15; // Minutes

    public static final String PERIODIC_RECURRING_PING_LORE_WORKER = "PERIODIC_RECURRING_PING_LORE_WORKER";
    public static final String ONE_TIME_LOCATION_FOREGROUND_LORE_WORKER = "ONE_TIME_LOCATION_FOREGROUND_LORE_WORKER";

    private Context mContext;
    private PreferenceUtil prefUtil;
    private RequestQueue requestQueue;
    private FusedLocationProviderClient fusedLocationProviderClient;


    /**
     * stores System.currentTimeMillis()
     */
    private long lastCheckedConfig = -1;
    private boolean waitingForIds = false;

    private LoreGeofence loreGeofence;

    private EventListener log;

    public LandmarksSDKWorker(Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.mContext = context;
        prefUtil = new PreferenceUtil(this.mContext);
    }

    @NonNull @Override
    public Result doWork() {

        log = EventLogger.getInstance().getEventListener();
        requestQueue = Volley.newRequestQueue(this.mContext);

        initDeviceIds();                // Start AsyncTask to obtain advertisingId (deviceId) and Firebase Instance ID (vendorId)
        initLocation();                 // Sets up location provider
        initFinalize();                 // Register listener and check analytics
        initRecurringLocationPings();   // Sets up WorkManager to ping location
        initGeofencingLore();           // Geofencing-based LORE

        return Result.success();
    }

    @SuppressLint("StaticFieldLeak")
    private void initDeviceIds() {
        // Firebase Vendor ID
        try {
            if(!prefUtil.hasString(Const.Preferences.VENDOR_ID)) {

                String uniqueID = UUID.randomUUID().toString();
                Timber.d("Vendor ID: %s", uniqueID);
                prefUtil.put(Const.Preferences.VENDOR_ID, uniqueID);
            }

        } catch (Exception ex) {
            Timber.d("Error: %s", ex.getLocalizedMessage());
            ex.printStackTrace();
            Sentry.captureException(ex);
        }


        // Google Device ID


        // checking device id for running analytics
        //if(!prefUtil.hasString(Const.Preferences.UUID)) {
//            if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this.mContext) != ConnectionResult.SUCCESS)
//                return;

            new AsyncTask<Void, Void, AdvertisingIdClient.Info>() {
                @Override
                protected AdvertisingIdClient.Info doInBackground(Void... params) {
                    AdvertisingIdClient.Info idInfo = null;

                    try {
                        idInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
                    } catch (GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    } catch (GooglePlayServicesRepairableException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    }

                    return idInfo;
                }

                @Override
                protected void onPostExecute(AdvertisingIdClient.Info idInfo) {
                    if(idInfo != null) {
                        prefUtil.put(Const.Preferences.UUID, idInfo.getId());
                        // to match with iOS  as in iOS true and false for adTrackingEnabled means the opposite as Android
                        prefUtil.put(Const.Preferences.AD_TRACKING, !idInfo.isLimitAdTrackingEnabled());
                        Timber.d("Obtained UUID: %s", idInfo.getId());

                        if(waitingForIds) {
                            //if (!prefUtil.hasString(Const.Preferences.UUID)) {
                            boolean checkAndroidIsEnabled = prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED);
                            if (checkAndroidIsEnabled = true) {
                                checkAnalytics();
                            }
                           // }
                        }
                    }
                }
            }.execute();
       // }
    }

    private void initLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.mContext);
    }

    private void initFinalize() {
        LandmarksIDManager.getInstance(this.mContext).setAppStateListener(this);
        if(log != null) log.onInit(TAG, "SDK Initialized");
        boolean checkAndroidIsEnabled = prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED);
        if (checkAndroidIsEnabled = true) {
            checkAnalytics();
        }
    }

    /**
     * 1. Sets up LoreWorker on schedule
     * 2. sends one time sore event NOW
     * 3. Sets up a schedule to send LORE event(currently every 4 hours)
     * 4. Sets up a schedule to construct geofence at that time's location(currently every 1 hour)
     */
    private void initRecurringLocationPings() {
        long timeInterval = prefUtil.getLongWithDefault(Const.Preferences.TIME_INTERVAL_MINS, DEFAULT_TIME_INTERVAL);
        if(timeInterval > 0) {
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(LoreWorker.class,
                    timeInterval, TimeUnit.MINUTES, timeInterval * 30, TimeUnit.SECONDS).build();

            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(PERIODIC_RECURRING_PING_LORE_WORKER, ExistingPeriodicWorkPolicy.REPLACE, workRequest);

        } else {
            if(log != null) log.msg(TAG, "Timer interval is 0. Not starting timer-based LORE");
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(PERIODIC_RECURRING_PING_LORE_WORKER);
        }

        // Wake-up calls using ScheduledThreadPoolExecutor

        ScheduledThreadPoolExecutor periodicThreadPoolExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        Runnable wakeUpPeriodic = new Runnable(){
            @Override
            public void run() {
                try {
                    Timber.d("Launching wake-up call");
                    sendSingleLore("wake-up");

                }catch(Exception e){
                    Timber.e("Error: %s", e.getMessage());
                    Sentry.captureException(e);
                    e.printStackTrace();
                }
            }
        };

        Timber.d("Schedule time for Periodic STPE: %s", DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()));
        periodicThreadPoolExecutor.scheduleAtFixedRate(wakeUpPeriodic, 1, 4, TimeUnit.HOURS);

        ScheduledThreadPoolExecutor geofenceThreadPoolExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        Runnable wakeUpGeofence = new Runnable() {
            @Override
            public void run() {
                try {
                    Timber.d("Launching geofence reset");

                    Timber.d("Resetting geofences");
                    if(loreGeofence != null) loreGeofence.remove();
                    initGeofencingLore();

                } catch(Exception e){
                    Timber.e("Error: %s", e.getMessage());
                    Sentry.captureException(e);
                    e.printStackTrace();
                }
            }
        };

        Timber.d("Schedule time for Geofence STPE: %s", DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()));
        geofenceThreadPoolExecutor.scheduleAtFixedRate(wakeUpGeofence, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Sets up GeoFence in current location
     */
    @SuppressLint("MissingPermission")
    private void initGeofencingLore() {
        loreGeofence = LoreGeofence.getInstance(getApplicationContext());
        long distanceInterval = prefUtil.getLong(Const.Preferences.DISTANCE_INTERVAL_METERS);
        if(distanceInterval > 0) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location == null) return;

                    Timber.d("Location received, initializing geofence");

                    prefUtil.put(Const.Preferences.LAST_LOC_LAT, location.getLatitude());
                    prefUtil.put(Const.Preferences.LAST_LOC_LONG, location.getLongitude());
                    prefUtil.put(Const.Preferences.LAST_LOC_TIME, location.getTime());

                    long radius = loreGeofence.getDynamicDistanceInterval(location.getSpeed());
                    Timber.d("Dynamic Geofence Radius: %s Speed: %s", radius, location.getSpeed());

                    loreGeofence.remove();
                    loreGeofence.addGeofence(location, radius);
                    loreGeofence.start();
                }
            });

        } else {
            if(log != null) log.msg(TAG, "Distance interval is 0. Not starting distance-based LORE");
            loreGeofence.remove();
        }
    }

    @Override
    public void onBackground() {
        Timber.d("onBackground()");
        boolean checkAndroidIsEnabled = prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED);
        if (checkAndroidIsEnabled = true) {
            checkAnalytics();
        }
    }

    @Override
    public void onForeground() {
        Timber.d("onForeground()");

        if(Utils.suppressCheckConfigMins(prefUtil, TAG ) == false) {
            checkConfig();
            boolean checkAndroidIsEnabled = prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED);
            if (checkAndroidIsEnabled) {
                checkAnalytics();
            }

            //reset the timer flag for checking onForeground
            prefUtil.put(Const.Preferences.LAST_FOREGROUND_CALL_DATE_TIME, DateTimeUtil.getCurrentDateTime());
        }
    }

    @Override
    public void onClosed() {
        Timber.d("onClosed()");
        EventBatcher.getInstance(this.mContext).flush();
    }

    @Override
    public void onStop() {
        EventBatcher.getInstance(this.mContext).stop();
        requestQueue.stop();

        if(log != null) log.msg(TAG, "SDK Service stopped");
    }

    private void sendSingleLore(String tag) {
        Timber.d("About to send single LORE. Tag: %s", tag);
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(LoreWorker.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(ONE_TIME_LOCATION_FOREGROUND_LORE_WORKER, ExistingWorkPolicy.KEEP, workRequest);
    }

    private void checkConfig() {
        if(LandmarksIDManager.getInstance(getApplicationContext()).isFreshStart())
            return;

        if(System.currentTimeMillis() - lastCheckedConfig < 3000)
            return;

        final ConfigListener configListener = LandmarksIDManager.getInstance(getApplicationContext()).getConfigListener();

        if(log != null) {
            log.msg(TAG, "Checking config...");
            log.msg(TAG, "Checking config from in Foreground in LO");
        }

        Timber.d("Checking config...");
        Timber.d("Checking config from in Foreground in LO");

        //this the hack for weatherzone while the config not returning back
        Boolean androidEnabled = prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED);
        if(androidEnabled) {
            initRecurringLocationPings();
            initGeofencingLore();
            sendSingleLore("config-check");
            checkAnalytics();
        }
        // make config flag false to skd use fresh config
        prefUtil.put(Const.Preferences.CONFIG_LOADED, false);
        Api.getDataCollectionConfig(this.mContext, requestQueue, new DataCollectionCallbackListener() {
            @Override
            public void onConfigReceived(boolean isAndroidEnabled) {
                if(log != null)
                    log.onConfig(prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED),
                            prefUtil.getLong(Const.Preferences.TIME_INTERVAL_MINS),
                            prefUtil.getLong(Const.Preferences.DISTANCE_INTERVAL_METERS),
                            prefUtil.getLong(Const.Preferences.MIN_SPEED_KPH),
                            prefUtil.getLong(Const.Preferences.MAX_SPEED_KPH));

                if(log != null) log.msg(TAG, "config received from sent config request by Checkconfig Foreground in LO");

                if(isAndroidEnabled) {
                    //the following block commented due to weatherzone issue for getting back config response
//                    initRecurringLocationPings();
//                    initGeofencingLore();
//                    sendSingleLore("config-check");
//                    checkAnalytics();

                    if(configListener != null)
                        configListener.onSuccess(true,
                                prefUtil.getLong(Const.Preferences.TIME_INTERVAL_MINS),
                                prefUtil.getLong(Const.Preferences.DISTANCE_INTERVAL_METERS),
                                prefUtil.getInt(Const.Preferences.BLUEDOT_MODE),
                                prefUtil.getLong(Const.Preferences.MIN_SPEED_KPH),
                                prefUtil.getLong(Const.Preferences.MAX_SPEED_KPH),
                                prefUtil.getString(Const.Preferences.MONITORED_APPS),
                                prefUtil.getLong(Const.Preferences.DISTANCE_FILTER_METERS),
                                prefUtil.getInt(Const.Preferences.BATCH_SIZE));

                } else {
                    if(log != null) log.msg(TAG, "Android disabled. Stopping everything");

                    if(configListener != null) configListener.onDisable();

                    LandmarksIDManager.getInstance(mContext).stopSDK(LandmarksSDKWorker.this.getApplicationContext());


                }
            }

            @Override
            public void onConfigError() {
            }
        });

        lastCheckedConfig = System.currentTimeMillis();
    }

    /**
     * Checks the previously stored device info data in SharedPreference
     * And sends the most recent info if anything has changes back to server
     */
    private void checkAnalytics() {

        Boolean configLoaded = prefUtil.getBoolean(Const.Preferences.CONFIG_LOADED);
        Timber.d("check Analytics get called ");
        // if sdk config not loaded from the back-end
        if(configLoaded == false) { return;}

        long lastCheckedAnalytics = prefUtil.getLong(Const.Preferences.LAST_CHECKED_ANALYTICS);
        Timber.d("3 seconds rule is checking ");
        if(System.currentTimeMillis() - lastCheckedAnalytics < 3000) return;

        Timber.d("3 seconds rule is passed ");

        if(!prefUtil.hasString(Const.Preferences.UUID) || !prefUtil.hasString(Const.Preferences.VENDOR_ID)) {
            Timber.d("Device ID or Vendor ID is empty. Flagging for later execution");
            waitingForIds = true;
            return;

        } else {
            Timber.d("All IDs available");
            waitingForIds = false;
        }

        if(log != null) log.msg(TAG, "Checking device and app info");
        Timber.d("Checking device and app info");

        AnalyticsUtil analyticsUtil = new AnalyticsUtil(this.mContext);
        JSONObject jsonObject = analyticsUtil.getJsonIfChanged(analyticsUtil.collect(), analyticsUtil.retrieveStored());

        Timber.d("got the result from get Json If Changed and result is: %s", jsonObject);

        if(jsonObject != null) {
            Timber.d("sendAnalyticsInfo get called");
            Timber.d("send Analytics Info's with %s", jsonObject.toString());
            Api.sendAnalyticsInfo(this.mContext, requestQueue, jsonObject);
            if(log != null) log.msg(TAG, "Info changed: " + jsonObject);
            Timber.d("Info changed: %s", jsonObject);

        } else {
            if (log != null) log.msg(TAG, "Nothing's changed. Not posting anything");
            Timber.d("Nothing's changed. Not posting anything");
        }

        prefUtil.put(Const.Preferences.LAST_CHECKED_ANALYTICS, System.currentTimeMillis());
    }
}
