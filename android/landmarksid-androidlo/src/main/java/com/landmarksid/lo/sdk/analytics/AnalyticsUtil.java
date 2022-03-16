package com.landmarksid.lo.sdk.analytics;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.permissions.PermissionUtil;
import com.landmarksid.lo.preferences.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.sentry.Sentry;
import timber.log.Timber;


public class AnalyticsUtil {
    private static final String TAG = "landmarks.analytics";
    private static Context context;

    public enum AppStatus {
        IMPORTANCE_FOREGROUND,
        IMPORTANCE_CACHED,
        IMPORTANCE_CANT_SAVE_STATE,
        IMPORTANCE_FOREGROUND_SERVICE,
        IMPORTANCE_GONE,
        IMPORTANCE_PERCEPTIBLE,
        IMPORTANCE_SERVICE,
        IMPORTANCE_TOP_SLEEPING,
        IMPORTANCE_VISIBLE,
        REASON_PROVIDER_IN_USE,
        REASON_SERVICE_IN_USE,
        REASON_UNKNOWN
    }

    public enum BatteryStatus {
        CHARGING,
        DISCHARGING,
        FULL,
        UNKNOWN
    }

    public enum NetworkStatus {
        WIFI,
        WIMAX,
        ETHERNET,
        BLUETOOTH,
        CDMA,
        _2G,
        _3G,
        _4G,
        _5G,
        LTE,
        UNKNOWN
    }


    public static class Analytics {
        private String clientId;
        private String appId;
        private String appVersion;
        private String customerId;

        private String vendorId;
        private String deviceId;
        private boolean adTrackingEnabled;
        private String locationPermission;

        private String deviceModel;
        private String deviceOs;
        private String deviceOsVersion;

        private JSONArray installedApps;

        private String simOperatorName;
        private String simOperatorIso;
        private String networkOperatorName;
        private String networkOperatorIso;
        private String customData;

        private String appSatus;

        @Override
        public boolean equals(Object o) {
            if (o instanceof Analytics) {
                Analytics a = (Analytics) o;

                return a.clientId.equals(this.clientId)
                        && a.appId.equals(this.appId)
                        && a.appVersion.equals(this.appVersion)
                        && a.customerId.equals(this.customerId)
                        && a.vendorId.equals(this.vendorId)
                        && a.deviceId.equals(this.deviceId)
                        && (a.adTrackingEnabled == this.adTrackingEnabled)
                        && a.locationPermission.equals(this.locationPermission)
                        && a.deviceModel.equals(this.deviceModel)
                        && a.deviceOs.equals(this.deviceOs)
                        && a.deviceOsVersion.equals(this.deviceOsVersion)
                        && a.installedApps.toString().equals(this.installedApps.toString())
                        && a.simOperatorName.equals(this.simOperatorName)
                        && a.simOperatorIso.equals(this.simOperatorIso)
                        && a.networkOperatorName.equals(this.networkOperatorName)
                        && a.networkOperatorIso.equals(this.networkOperatorIso)
                        && a.customData.equals(this.customData);
            }

            return false;
        }

        @Override
        @NonNull
        public String toString() {
            try {
                JSONObject res = new JSONObject();
                res.put(Const.Fields.CLIENT_ID, this.clientId);
                res.put(Const.Fields.APP_ID, this.appId);
                res.put(Const.Fields.APP_VERSION, this.appVersion);
                res.put(Const.Fields.CUSTOMER_ID, this.customerId);
                res.put(Const.Fields.VENDOR_ID, this.vendorId);
                res.put(Const.Fields.DEVICE_ID, this.deviceId);
                res.put(Const.Fields.AD_TRACKING_ENABLED, this.adTrackingEnabled);
                res.put(Const.Fields.LOCATION_PERMISSION, this.locationPermission);
                res.put(Const.Fields.DEVICE_MODEL, this.deviceModel);
                res.put(Const.Fields.DEVICE_OS, this.deviceOs);
                res.put(Const.Fields.OS_VERSION, this.deviceOsVersion);
                res.put(Const.Fields.INSTALLED_APPS, this.installedApps);
                res.put(Const.Fields.SIM_OPERATOR_NAME, this.simOperatorName);
                res.put(Const.Fields.SIM_OPERATOR_ISO, this.simOperatorIso);
                res.put(Const.Fields.NETWORK_OPERATOR_NAME, this.networkOperatorName);
                res.put(Const.Fields.NETWORK_OPERATOR_ISO, this.networkOperatorIso);
                res.put(Const.Fields.CUSTOM_DATA, this.customData);

                return res.toString();

            } catch (JSONException ex) {
                Sentry.captureException(ex);
                return "JSON Error: " + ex.getLocalizedMessage();
            }
        }
    }

    private PreferenceUtil prefUtil;
    private boolean hasMinPermission, hasBackgroundPermissions;
    private JSONArray installedApps;
    private TelephonyManager telephonyManager;

    public AnalyticsUtil(Context context) {

        AnalyticsUtil.context = context;
        prefUtil = new PreferenceUtil(context);

        hasMinPermission = PermissionUtil.hasAllPermissions(context, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10. Explicit background location permissions
            hasBackgroundPermissions = PermissionUtil.hasAllPermissions(context, new String[]{
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            });
        }

        installedApps = MonitoredAppsUtil.getInstalledApps(prefUtil.getString(Const.Preferences.MONITORED_APPS),
                context.getPackageManager());


        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public JSONObject getJsonIfChanged(Analytics newData, Analytics oldData) {

        Timber.d("getJsonIfChanged get called");
        Timber.d("Check Device Info for getJsonIfChanged for: %s new Data: %s", oldData.toString(), newData.toString());
        if (!newData.equals(oldData)) {
            try {
                JSONObject res = new JSONObject();
                res.put(Const.Fields.CLIENT_ID, newData.clientId);
                res.put(Const.Fields.APP_ID, newData.appId);
                res.put(Const.Fields.APP_VERSION, newData.appVersion);
                res.put(Const.Fields.CUSTOMER_ID, newData.customerId);
                res.put(Const.Fields.VENDOR_ID, newData.vendorId);
                res.put(Const.Fields.DEVICE_ID, newData.deviceId);
                res.put(Const.Fields.AD_TRACKING_ENABLED, newData.adTrackingEnabled);
                res.put(Const.Fields.LOCATION_PERMISSION, newData.locationPermission);
                res.put(Const.Fields.DEVICE_MODEL, newData.deviceModel);
                res.put(Const.Fields.DEVICE_OS, newData.deviceOs);
                res.put(Const.Fields.OS_VERSION, newData.deviceOsVersion);
                Timber.d("Monitored Apps");
                Timber.d(prefUtil.getString(Const.Preferences.MONITORED_APPS));
                res.put(Const.Fields.INSTALLED_APPS, MonitoredAppsUtil.getInstalledApps(prefUtil.getString(Const.Preferences.MONITORED_APPS),
                        AnalyticsUtil.context.getPackageManager()));
                res.put(Const.Fields.SIM_OPERATOR_NAME, newData.simOperatorName);
                res.put(Const.Fields.SIM_OPERATOR_ISO, newData.simOperatorIso);
                res.put(Const.Fields.NETWORK_OPERATOR_NAME, newData.networkOperatorName);
                res.put(Const.Fields.NETWORK_OPERATOR_ISO, newData.networkOperatorIso);
                if (newData.customData != null && !newData.customData.isEmpty())
                    res.put(Const.Fields.CUSTOM_DATA, new JSONArray(newData.customData));

                Timber.d(String.valueOf(MonitoredAppsUtil.getInstalledApps(prefUtil.getString(Const.Preferences.MONITORED_APPS),
                        AnalyticsUtil.context.getPackageManager())));

                save(newData);

                Timber.d("Device Info changed. New: %s", res);


                return res;

            } catch (JSONException ex) {
                Sentry.captureException(ex);
            }
        }

        return null;
    }

    public Analytics collect() {
        Analytics analytics = new Analytics();

        Timber.d("In Analytics. Permissions answered: %s has basic permissions: %s", prefUtil.getBoolean(Const.Preferences.PERMISSIONS_ANSWERED), hasMinPermission);

        //TODO: should have done in better way: this is for the use cases a client already asked for location permission and no need ask by SDK
        prefUtil.put(Const.Preferences.PERMISSIONS_ANSWERED, true);

        analytics.clientId = "";
        analytics.appId = prefUtil.getString(Const.Preferences.APP_ID);
        analytics.appVersion = prefUtil.getString(Const.Preferences.APP_VERSION);
        analytics.customerId = prefUtil.getString(Const.Preferences.CUSTOMER_ID);
        analytics.customData = prefUtil.getString(Const.Preferences.CUSTOM_DATA);

        analytics.vendorId = prefUtil.getString(Const.Preferences.VENDOR_ID);
        analytics.deviceId = prefUtil.getString(Const.Preferences.UUID);
        analytics.adTrackingEnabled = prefUtil.getBoolean(Const.Preferences.AD_TRACKING);
        analytics.locationPermission = !prefUtil.getBoolean(Const.Preferences.PERMISSIONS_ANSWERED) ?
                "unknown" : hasMinPermission ? "granted" : "denied";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10. Handle "whilst in use" case
            if (prefUtil.getBoolean(Const.Preferences.PERMISSIONS_ANSWERED) && hasMinPermission && !hasBackgroundPermissions)
                analytics.locationPermission = "whenInUse";
        }

        analytics.deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        analytics.deviceOs = "Android";
        analytics.deviceOsVersion = Build.VERSION.RELEASE;

        analytics.installedApps = this.installedApps;

        analytics.simOperatorName = this.telephonyManager.getSimOperatorName();
        analytics.simOperatorIso = this.telephonyManager.getSimCountryIso();
        analytics.networkOperatorName = this.telephonyManager.getNetworkOperatorName();
        analytics.networkOperatorIso = this.telephonyManager.getNetworkCountryIso();

        return analytics;
    }

    /**
     * Retrieves from preference ans constructs a new Analytics object
     * @return
     */
    public Analytics retrieveStored() {
        Analytics analytics = new Analytics();
        Timber.d("retrieve Stored info in user preferences ");

        analytics.clientId = prefUtil.getString(Const.Preferences.CLIENT_ID_PREV);
        analytics.appId = prefUtil.getString(Const.Preferences.APP_ID_PREV);
        analytics.appVersion = prefUtil.getString(Const.Preferences.APP_VERSION_PREV);
        analytics.customerId = prefUtil.getString(Const.Preferences.CUSTOMER_ID_PREV);

        analytics.vendorId = prefUtil.getString(Const.Preferences.VENDOR_ID_PREV);
        analytics.deviceId = prefUtil.getString(Const.Preferences.DEVICE_ID_PREV);
        analytics.adTrackingEnabled = prefUtil.getBoolean(Const.Preferences.AD_TRACKING_PREV);
        analytics.locationPermission = prefUtil.getString(Const.Preferences.LOCATION_PERMISSION_PREV);

        analytics.deviceModel = prefUtil.getString(Const.Preferences.DEVICE_MODEL_PREV);
        analytics.deviceOs = prefUtil.getString(Const.Preferences.DEVICE_OS_PREV);
        analytics.deviceOsVersion = prefUtil.getString(Const.Preferences.OS_VERSION_PREV);

        try {
            analytics.installedApps = new JSONArray(prefUtil.getStringWithDefault(Const.Preferences.INSTALLED_APPS_PREV, "[landmarksTest]"));
            Timber.d("check previously installed app if is empty default is [] %s", analytics.installedApps.toString());
        } catch (JSONException ignored) {
            Timber.d("Error for serializing installed app %s", ignored.getLocalizedMessage());
        }

        analytics.simOperatorName = prefUtil.getString(Const.Preferences.SIM_OPERATOR_NAME_PREV);
        analytics.simOperatorIso = prefUtil.getString(Const.Preferences.SIM_OPERATOR_ISO_PREV);
        analytics.networkOperatorName = prefUtil.getString(Const.Preferences.NETWORK_OPERATOR_NAME_PREV);
        analytics.networkOperatorIso = prefUtil.getString(Const.Preferences.NETWORK_OPERATOR_ISO_PREV);
        analytics.customData = prefUtil.getString(Const.Preferences.CUSTOM_DATA_PREV);

        return analytics;
    }

    private void save(Analytics analytics) {
        prefUtil.put(Const.Preferences.CLIENT_ID_PREV, analytics.clientId);
        prefUtil.put(Const.Preferences.APP_ID_PREV, analytics.appId);
        prefUtil.put(Const.Preferences.APP_VERSION_PREV, analytics.appVersion);
        prefUtil.put(Const.Preferences.CUSTOMER_ID_PREV, analytics.customerId);

        prefUtil.put(Const.Preferences.VENDOR_ID_PREV, analytics.vendorId);
        prefUtil.put(Const.Preferences.DEVICE_ID_PREV, analytics.deviceId);
        prefUtil.put(Const.Preferences.AD_TRACKING_PREV, analytics.adTrackingEnabled);
        prefUtil.put(Const.Preferences.LOCATION_PERMISSION_PREV, analytics.locationPermission);

        prefUtil.put(Const.Preferences.DEVICE_MODEL_PREV, analytics.deviceModel);
        prefUtil.put(Const.Preferences.DEVICE_OS_PREV, analytics.deviceOs);
        prefUtil.put(Const.Preferences.OS_VERSION_PREV, analytics.deviceOsVersion);

        prefUtil.put(Const.Preferences.INSTALLED_APPS_PREV, analytics.installedApps);

        prefUtil.put(Const.Preferences.SIM_OPERATOR_NAME_PREV, analytics.simOperatorName);
        prefUtil.put(Const.Preferences.SIM_OPERATOR_ISO_PREV, analytics.simOperatorIso);
        prefUtil.put(Const.Preferences.NETWORK_OPERATOR_NAME_PREV, analytics.networkOperatorName);
        prefUtil.put(Const.Preferences.NETWORK_OPERATOR_ISO_PREV, analytics.networkOperatorIso);
        prefUtil.put(Const.Preferences.CUSTOM_DATA_PREV, analytics.customData);
    }

    public AppStatus getAppStatus() {

        ActivityManager aManager = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> appProcesses = aManager.getRunningAppProcesses();
        if (appProcesses == null) return AppStatus.REASON_UNKNOWN;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ) {
                    return AppStatus.IMPORTANCE_FOREGROUND;
                } else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
                    return AppStatus.IMPORTANCE_CACHED;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE) {
                    return AppStatus.IMPORTANCE_CANT_SAVE_STATE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE) {
                    return AppStatus.IMPORTANCE_FOREGROUND_SERVICE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE) {
                    return AppStatus.IMPORTANCE_GONE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
                    return AppStatus.IMPORTANCE_PERCEPTIBLE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                    return AppStatus.IMPORTANCE_SERVICE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING) {
                    return AppStatus.IMPORTANCE_TOP_SLEEPING;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    return AppStatus.IMPORTANCE_VISIBLE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.REASON_PROVIDER_IN_USE) {
                    return AppStatus.REASON_PROVIDER_IN_USE;
                }  else if (appProcess.importance == ActivityManager.RunningAppProcessInfo.REASON_SERVICE_IN_USE) {
                    return AppStatus.REASON_SERVICE_IN_USE;
                }
                break;
            }
        }
        return AppStatus.REASON_UNKNOWN;
    }


    public BatteryStatus getBatteryStatus() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        if (isCharging == true) {
            return BatteryStatus.CHARGING;
        }

        boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;
        if (isFull == true) {
            return BatteryStatus.FULL;
        }

        boolean isDischarging = status == BatteryManager.BATTERY_STATUS_DISCHARGING;
        if (isDischarging == true) {
            return BatteryStatus.DISCHARGING;
        }

        return BatteryStatus.UNKNOWN;
    }


    public float getBatteryLevel() {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float) scale;

        return batteryPct;
    }

    public NetworkStatus getNetworkStatus() {

        //connected to the WIFI
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            // connected to the internet
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return NetworkStatus.WIFI;
                default:
                    break;
            }
        }
        //connected to the Mobile
        if (activeNetwork.getSubtypeName().equals("IDEN")) {
            return NetworkStatus._2G;
        } else if (activeNetwork.getSubtypeName().equals("HSPAP")) {
            return NetworkStatus._3G;
        } else if (activeNetwork.getSubtypeName().equals("LTE")) {
            return NetworkStatus._4G;
        } else if (activeNetwork.getSubtypeName().equals("NR")) {
            return NetworkStatus._5G;
        }

        return NetworkStatus.UNKNOWN;
    }

}
