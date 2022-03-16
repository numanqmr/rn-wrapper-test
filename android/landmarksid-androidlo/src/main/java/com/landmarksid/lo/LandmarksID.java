package com.landmarksid.lo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.toolbox.Volley;
import com.landmarksid.lo.formats.CustomData;
import com.landmarksid.lo.listeners.ConfigListener;
import com.landmarksid.lo.listeners.DataCollectionCallbackListener;
import com.landmarksid.lo.listeners.EventListener;
import com.landmarksid.lo.permissions.Permission;
import com.landmarksid.lo.permissions.PermissionUtil;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.Api;
import com.landmarksid.lo.sdk.EventLogger;
import com.landmarksid.lo.sdk.LandmarksSDKWorker;
import com.landmarksid.lo.sdk.lore.LoreWorker;

import org.json.JSONException;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import timber.log.Timber;

public class LandmarksID {
    private static final String TAG = "landmarks.init";

    private static final String ONE_TIME_START_LANDMARK_START_SERVICE_LANDMARKS_SDK__WORKER = "ONE_TIME_START_LANDMARK_START_SERVICE_LANDMARKS_SDK__WORKER";
    private static final String ONE_TIME_LOCATION_PERMISSION_REQUEST_LORE_WORKER = "ONE_TIME_LOCATION_PERMISSION_CHANGED_LORE_WORKER";

    private static final String ONE_TIME_ON_CONFIG_RECEIVED_LANDMARKS_SDK_WORKER = "ONE_TIME_ON_CONFIG_RECEIVED_LANDMARKS_SDK_WORKER";

    private static final String ONE_TIME_ON_CONFIG_ERROR_RECEIVED_LANDMARKS_SDK_WORKER = "ONE_TIME_ON_CONFIG_RECEIVED_ERROR_LANDMARKS_SDK_WORKER";

    private static volatile LandmarksID instance;
    private LandmarksID.Options options;

    private OneTimeWorkRequest sdkWorkRequest;
    private Permission locationPermissions;

    private PreferenceUtil prefUtil;
    private EventListener log;

    private boolean serviceStarted = false;

    private LandmarksID() {
        if(instance != null)
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
    }

    public static LandmarksID getInstance() {
        if(instance == null)
            synchronized (LandmarksID.class) {
                if(instance == null) {
                    instance = new LandmarksID();
                }
            }

        return instance;
    }

    /**
     *
     * @deprecated Do not use this method!
     */
    @Deprecated
    public LandmarksID start(Context context) {
        if(this.options == null) throw new IllegalArgumentException("Attempting to start SDK without properly initialized Options." +
                "Use constructor start(Context, LandmarksID.Options, [EventListener]) instead.");

        return start(context, this.options, null);
    }

    /**
     *
     * @deprecated Do not use this method!
     */
    @Deprecated
    public LandmarksID start(Context context, EventListener eventListener) {
        if(this.options == null) throw new IllegalArgumentException("Attempting to start SDK without properly initialized Options." +
            "Use constructor start(Context, LandmarksID.Options, [EventListener]) instead.");

        return start(context, this.options, eventListener);
    }

    public LandmarksID start(Context context, @NonNull Options options) {
        return start(context, options, null);
    }

    public LandmarksID start(Context context, @NonNull Options options, EventListener eventListener) {
        return start(context, options, eventListener, null);
    }

    public LandmarksID start(Context context, @NonNull Options options, EventListener eventListener, ConfigListener configListener) {
        if(BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        //Call LandmarkSDKWorker
        this.sdkWorkRequest = new OneTimeWorkRequest.Builder(LandmarksSDKWorker.class).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(ONE_TIME_START_LANDMARK_START_SERVICE_LANDMARKS_SDK__WORKER,
                ExistingWorkPolicy.KEEP, this.sdkWorkRequest);

        LandmarksIDManager.getInstance(context);

        if(configListener != null)
            LandmarksIDManager.getInstance(context).setConfigListener(configListener);

        if(eventListener != null)
            EventLogger.getInstance().setEventListener(eventListener);

        instance.log = EventLogger.getInstance().getEventListener();
        instance.options = options;

        if(log != null) log.msg(TAG, "Starting SDK with options...");

        prefUtil = new PreferenceUtil(context);
        prefUtil.put(Const.Extras.API_KEY, options.apiKey);
        prefUtil.put(Const.Preferences.APP_ID, options.appId);
        prefUtil.put(Const.Preferences.CUSTOMER_ID, options.customerId);
        prefUtil.put(Const.Preferences.APP_SECRET, options.appSecret);
        prefUtil.put(Const.Preferences.DEBUG_MODE, options.debugMode);

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), 0);
            prefUtil.put(Const.Preferences.APP_VERSION, packageInfo.versionName);

        } catch (PackageManager.NameNotFoundException ignored) { }

        try {
            if(options.customData != null)
                prefUtil.put(Const.Preferences.CUSTOM_DATA, options.customData.getJson());
            else
                prefUtil.remove(Const.Preferences.CUSTOM_DATA);

        } catch(JSONException ignored) { }

        instance.locationPermissions = Permission.NA;

        if(!serviceStarted)
            startService(context);


        return instance;
    }

    public void stop(Context context) {
        try {
            if (log != null) log.msg(TAG, "Stopping SDK...");

            LandmarksIDManager.getInstance(context).stopSDK(context);

            WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(LandmarksSDKWorker.ONE_TIME_LOCATION_FOREGROUND_LORE_WORKER);

            serviceStarted = false;

        } catch (Exception ex) {
            Sentry.captureException(ex);
        }
    }

    private void initLocationPermissions(Activity activity) {
        if(!(activity instanceof ActivityCompat.OnRequestPermissionsResultCallback)) {
            RuntimeException ex = new RuntimeException("Activity must implement ActivityCompat.OnRequestPermissionsResultCallback " +
                    "and include a call to LandmarksID.onRequestPermissionsResult(Context, int, String[], int[])");

            Sentry.captureException(ex);
            throw ex;
        }

        if(prefUtil == null)
            prefUtil = new PreferenceUtil(activity.getApplicationContext());

        String[] reqPerms = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean hasMinPermissions;

        // Are at least "Whilst in Use" permissions granted?
        hasMinPermissions = PermissionUtil.hasAllPermissions(activity, reqPerms); // Are at least "Whilst in Use" permissions granted?

        if(prefUtil.getBoolean(Const.Preferences.PERMISSIONS_REQUESTED) == true) {
            hasMinPermissions = true;
        }

        if(!hasMinPermissions && instance.locationPermissions == Permission.NA && prefUtil.getBoolean(Const.Preferences.LOCATION_USABLE)) {
            // Possible restoration from backup. Permission were previously granted, but somehow none are active. Reset all keys
            prefUtil.put(Const.Preferences.PERMISSIONS_ANSWERED, false);
            prefUtil.put(Const.Preferences.PERMISSIONS_REQUESTED, false);
            prefUtil.put(Const.Preferences.LOCATION_USABLE, false);
        }

        if(!hasMinPermissions && !prefUtil.getBoolean(Const.Preferences.PERMISSIONS_REQUESTED)) {
            PermissionUtil.requestPermission(activity, Const.RequestCodes.LOCATION_PERMISSIONS, reqPerms); // we check

            instance.locationPermissions = Permission.REQUESTED;
            prefUtil.put(Const.Preferences.PERMISSIONS_REQUESTED, true);

        } else if(hasMinPermissions) {
            // At least "Whilst in use" permissions are granted. Check for Always (i.e. background, if Android 10 or later)
            instance.locationPermissions = Permission.GRANTED;
            Timber.d("Basic permissions granted");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean isBackgroundGranted = PermissionUtil.hasAllPermissions(activity, new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION });
                if(!isBackgroundGranted)
                    instance.locationPermissions = Permission.WHILST_IN_USE;

                Timber.d("Android 10. Background permissions: %s", isBackgroundGranted);
            }

            prefUtil.put(Const.Preferences.PERMISSIONS_ANSWERED, true);
        }

    }

    public void onRequestPermissionsResult(Context context, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode != Const.RequestCodes.LOCATION_PERMISSIONS) return;
        if(prefUtil == null) prefUtil = new PreferenceUtil(context);

        instance.locationPermissions = PermissionUtil.isPermissionGranted(permissions, grantResults, new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        // Android 10. Check if background location is also granted. If so, this means "Always". If not, only "Whilst in use".
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Permission background = PermissionUtil.isPermissionGranted(permissions, grantResults, new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION });
            if(background == Permission.DENIED && instance.locationPermissions == Permission.GRANTED)
                instance.locationPermissions = Permission.WHILST_IN_USE;
        }

        prefUtil.put(Const.Preferences.PERMISSIONS_ANSWERED, true);

        if(instance.locationPermissions == Permission.GRANTED || instance.locationPermissions == Permission.WHILST_IN_USE) {
            prefUtil.put(Const.Preferences.LOCATION_USABLE, true);
            startService(context);

            Timber.d("Single LORE at onResume()");
            //Call single location event once app location permission is changed
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(LoreWorker.class).build();
            WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(ONE_TIME_LOCATION_PERMISSION_REQUEST_LORE_WORKER,
                    ExistingWorkPolicy.KEEP, workRequest);
        }

        Timber.d("Permission result callback. Level: %s", instance.locationPermissions);
    }

    public void setLocationUsable(Activity activity) {
        initLocationPermissions(activity);

        if(serviceStarted) {

            LandmarksIDManager.getInstance(activity.getApplicationContext()).stopSDK(activity.getApplicationContext());

            startService(activity);
        }
    }

    private void startService(final Context context) {
        Timber.d("Checking config at full init");

        if(prefUtil == null)
            prefUtil = new PreferenceUtil(context);

        if(!LandmarksIDManager.getInstance(context).isFreshStart())
            return;

        final ConfigListener configListener = LandmarksIDManager.getInstance(context).getConfigListener();
        // make config flag false to sdk use fresh config
        prefUtil.put(Const.Preferences.CONFIG_LOADED, false);
        Api.getDataCollectionConfig(context, Volley.newRequestQueue(context), new DataCollectionCallbackListener() {
            @Override
            public void onConfigReceived(boolean isAndroidEnabled) {
                if(log != null)
                    log.onConfig(prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED),
                            prefUtil.getLong(Const.Preferences.TIME_INTERVAL_MINS),
                            prefUtil.getLong(Const.Preferences.DISTANCE_INTERVAL_METERS),
                            prefUtil.getLong(Const.Preferences.MIN_SPEED_KPH),
                            prefUtil.getLong(Const.Preferences.MAX_SPEED_KPH));

                if(isAndroidEnabled) {
                    if(log != null) log.msg(TAG, "Config received. Starting service...");

                    // check sentry init eligibility
                    Boolean enableSentry = prefUtil.getBoolean(Const.Preferences.ENABLE_SENTRY);
                    if (enableSentry == false) {
                        SentryOptions options = new SentryOptions();
                        options.setDsn(Const.Endpoints.SENTRY_DSN);
                        options.setRelease(BuildConfig.VERSION_NAME);
                        options.setEnvironment(context.getApplicationContext().getPackageName());
                        options.setBeforeSend( (event, hint) -> {
                            Timber.d(event.toString());
                            for(SentryException exception: event.getExceptions()) {
                                for (SentryStackFrame frame : exception.getStacktrace().getFrames()) {
                                    if (frame.getModule().startsWith("com.landmarksid")) {
                                        return event;
                                    }
                                }
                            }
                            return null;
                        });
                        Sentry.init(options);
                        //Sentry.captureException(new Throwable("Sentry Capture"));
                    }
                    // disable sentry
                    else  {
                        Sentry.init(""); //DSN is required. Use empty string to disable SDK.
                    }

                    // start landmark worker
                    sdkWorkRequest = new OneTimeWorkRequest.Builder(LandmarksSDKWorker.class).build();
                    WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(ONE_TIME_ON_CONFIG_RECEIVED_LANDMARKS_SDK_WORKER,
                            ExistingWorkPolicy.KEEP, sdkWorkRequest);


                    serviceStarted = true;

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
                    if(log != null) log.msg(TAG, "Android disabled. No services shall be started");

                    if(configListener != null) configListener.onDisable();
                    stop(context);
                }
            }

            @Override
            public void onConfigError() {
                if(log != null) log.msg(TAG, "Config missing. Starting service with default values...");

                // start worker
                if(sdkWorkRequest == null) {
                    sdkWorkRequest = new OneTimeWorkRequest.Builder(LandmarksSDKWorker.class).build();
                }

                WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(ONE_TIME_ON_CONFIG_ERROR_RECEIVED_LANDMARKS_SDK_WORKER,
                        ExistingWorkPolicy.KEEP, sdkWorkRequest);


                serviceStarted = true;

                if(configListener != null) configListener.onError();
            }
        });
    }

    public void setCustomData(CustomData customData) {
        this.options.setCustomData(customData);
        try {
            if(options.customData != null)
                prefUtil.put(Const.Preferences.CUSTOM_DATA, options.customData.getJson());
            else
                prefUtil.remove(Const.Preferences.CUSTOM_DATA);

        } catch(JSONException ignored) { }
    }

    public static class Options {
        private String apiKey;
        private String appId, appSecret;
        private String customerId;
        private boolean debugMode = false;
        private CustomData customData;

        public Options setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Options setCustomerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Options setAppMetadata(String appId, String appSecret) {
            this.appId = appId;
            this.appSecret = appSecret;
            return this;
        }

        public Options setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        public Options setCustomData(CustomData customData) {
            this.customData = customData;
            return this;
        }
    }
}
