package com.landmarksid.lo.sdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.landmarksid.lo.BuildConfig;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.listeners.DataCollectionCallbackListener;
import com.landmarksid.lo.listeners.EventListener;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.analytics.AnalyticsUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.sentry.Sentry;
import timber.log.Timber;

public class Api {
    private static final String TAG = "landmarks.api";

    private static JSONObject getCommonRequestObject(Context context) throws JSONException {
        PreferenceUtil prefUtil = new PreferenceUtil(context);
        JSONObject request = new JSONObject();

        request.put(Const.Fields.SCHEMA_VERSION, 1.0);
        request.put(Const.Fields.APP_ID, prefUtil.getString(Const.Preferences.APP_ID));
        request.put(Const.Fields.DEVICE_ID, prefUtil.getString(Const.Preferences.UUID));
        request.put(Const.Fields.SDK_VERSION, BuildConfig.VERSION_NAME);
        request.put(Const.Fields.DEVICE_TYPE, Const.Values.DEVICE_TYPE);
        request.put(Const.Fields.OS_VERSION, Build.VERSION.RELEASE);
        request.put(Const.Fields.APP_BUILD_VERSION, prefUtil.getString(Const.Preferences.APP_VERSION));
        request.put(Const.Fields.AD_TRACKING_ENABLED, prefUtil.getBoolean(Const.Preferences.AD_TRACKING));
        request.put(Const.Fields.CLIENT_ID, prefUtil.getString(Const.Preferences.CLIENT_ID_PREV));
        request.put(Const.Fields.VENDOR_ID, prefUtil.getString(Const.Preferences.VENDOR_ID_PREV));
        request.put(Const.Fields.CUSTOMER_ID, prefUtil.getString(Const.Preferences.CUSTOMER_ID_PREV));

        if(prefUtil.hasString(Const.Preferences.CUSTOM_DATA) && !prefUtil.getString(Const.Preferences.CUSTOM_DATA).isEmpty())
            request.put(Const.Fields.CUSTOM_DATA, new JSONArray(prefUtil.getString(Const.Preferences.CUSTOM_DATA)));

        return request;
    }

    static void sendAnalyticsInfo(Context context, RequestQueue requestQueue, JSONObject request) {
        EventListener log = EventLogger.getInstance().getEventListener();
        if(log != null) log.msg(TAG, "Posting analytics info to server: " + request);

        Timber.d("Posting analytics info to server: %s", request);

        addPostRequestToQueue(context, requestQueue, Const.Endpoints.DEVICE_INFO, request);
    }

    public static JSONObject getJsonRequestLO(Context context, String eventTime, String eventId, double latitude, double longitude,
                                              float deviceSpeed, double accuracy, double altitude, String eventTrigger) {
        if(!new PreferenceUtil(context).hasString(Const.Preferences.UUID)) return null;

        try {
            JSONObject request = getCommonRequestObject(context);

            request.put(Const.Fields._ID, eventId);
            request.put(Const.Fields.MESSAGE_TYPE, Const.Values.MESSAGE_TYPE);
            request.put(Const.Fields.EVENT_TIME, eventTime);
            request.put(Const.Fields.SOURCE_EVENT_ID, eventId);
            request.put(Const.Fields.SOURCE, Const.Values.SOURCE_LANDMARKS);
            request.put(Const.Fields.LAT, latitude);
            request.put(Const.Fields.LONG, longitude);
            request.put(Const.Fields.DEVICE_SPEED, deviceSpeed);
            request.put(Const.Fields.HORIZONTAL_ACCURACY, accuracy);
            request.put(Const.Fields.VERTICAL_ACCURACY, 0.0);
            request.put(Const.Fields.ALTITUDE, altitude);
            request.put(Const.Fields.EVENT_TRIGGER, eventTrigger);
            // meta data about device battery and connectivity
            AnalyticsUtil analyticsUtil = new AnalyticsUtil(context);

            request.put(Const.Fields.APP_SATE, analyticsUtil.getAppStatus());
            request.put(Const.Fields.BATTERY_STATUS, analyticsUtil.getBatteryStatus());
            request.put(Const.Fields.BATTERY_LEVEL, analyticsUtil.getBatteryLevel());
            request.put(Const.Fields.NETWORK_STATUS, analyticsUtil.getNetworkStatus());

            // motion activity recognition from user preference
            String motionActivity = new PreferenceUtil(context).getString(Const.Preferences.MOTION_ACTIVITY);
            if(motionActivity.isEmpty()) {
                motionActivity = "UNKNOWN";
            }
            request.put(Const.Fields.MOTION_ACTIVITY, motionActivity);

            return request;

        } catch (JSONException ex) {
            Sentry.captureException(ex);
            return null;
        }
    }

    public static void postLOEvents(Context context, RequestQueue requestQueue, JSONObject request) {
        EventListener log = EventLogger.getInstance().getEventListener();
        if(log != null) log.msg(TAG, "Posting LO events to server");

        Timber.d("Posting LO events to server");

        addPostRequestToQueue(context, requestQueue, Const.Endpoints.LOCATION_PINGS, request);
    }

    public static void getDataCollectionConfig(Context context, RequestQueue requestQueue, final DataCollectionCallbackListener listener) {
        final EventListener log = EventLogger.getInstance().getEventListener();
        final PreferenceUtil prefUtil = new PreferenceUtil(context);

        Timber.d("Requesting config...");
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Const.Fields.SDK_VERSION, BuildConfig.VERSION_NAME);
        map.put(Const.Fields.OS_VERSION, Build.VERSION.RELEASE);
        try {
            map.put(Const.Fields.APP_BUILD_VERSION, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }

        addGetRequestToQueue(context, requestQueue, Const.Endpoints.LOCATION_PING_CONFIG, prefUtil.getString(Const.Preferences.APP_ID) + "?"+ Utils.urlEncodeUTF8(map),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject body = response.getJSONObject(Const.Fields.BODY);
                            Timber.d("Success. Config received %s", response.toString());

                            prefUtil.put(Const.Preferences.CONFIG_LOADED, true);

                            if(body.has(Const.Fields.ANDROID_ENABLED))
                                prefUtil.put(Const.Preferences.ANDROID_ENABLED, body.getBoolean(Const.Fields.ANDROID_ENABLED)); // TODO Flip here for testing

                            if(body.has(Const.Fields.TIME_INTERVAL_MINS))
                                prefUtil.put(Const.Preferences.TIME_INTERVAL_MINS, body.getLong(Const.Fields.TIME_INTERVAL_MINS));

                            if(body.has(Const.Fields.DISTANCE_INTERVAL_METERS))
                                prefUtil.put(Const.Preferences.DISTANCE_INTERVAL_METERS, body.getLong(Const.Fields.DISTANCE_INTERVAL_METERS));

                            if(body.has(Const.Fields.DISCOVERY_MODE) && !body.isNull(Const.Fields.DISCOVERY_MODE))
                                prefUtil.put(Const.Preferences.BLUEDOT_MODE, body.getInt(Const.Fields.DISCOVERY_MODE));

                            if(body.has(Const.Fields.MIN_SPEED_KPH))
                                prefUtil.put(Const.Preferences.MIN_SPEED_KPH, body.getLong(Const.Fields.MIN_SPEED_KPH));

                            if(body.has(Const.Fields.MAX_SPEED_KPH))
                                prefUtil.put(Const.Preferences.MAX_SPEED_KPH, body.getLong(Const.Fields.MAX_SPEED_KPH));

                            if(body.has(Const.Fields.MONITORED_APPS))
                                prefUtil.put(Const.Preferences.MONITORED_APPS, body.getString(Const.Fields.MONITORED_APPS));

                            if(body.has(Const.Fields.DISTANCE_FILTER_METERS))
                                prefUtil.put(Const.Preferences.DISTANCE_FILTER_METERS, body.getLong(Const.Fields.DISTANCE_FILTER_METERS));

                            if(body.has(Const.Fields.ENABLE_MOTION_ACTIVITY))
                                prefUtil.put(Const.Preferences.ENABLE_MOTION_ACTIVITY, body.getBoolean(Const.Fields.ENABLE_MOTION_ACTIVITY));

                            if(body.has(Const.Fields.ENABLE_SENTRY))
                                prefUtil.put(Const.Preferences.ENABLE_SENTRY, body.getBoolean(Const.Fields.ENABLE_SENTRY));

                            if(body.has(Const.Fields.SUPPRESS_CHECK_CONFIG_MINS))
                                prefUtil.put(Const.Preferences.SUPPRESS_CHECK_CONFIG_MINS, body.getInt(Const.Fields.SUPPRESS_CHECK_CONFIG_MINS));

                            prefUtil.put(Const.Preferences.BATCH_SIZE,
                                    body.has(Const.Fields.BATCH_SIZE)? body.getInt(Const.Fields.BATCH_SIZE) : 10);

                            listener.onConfigReceived(body.getBoolean(Const.Fields.ANDROID_ENABLED)); // TODO Flip here for testing

                            Timber.d("Success. Config received and saved. Enabled: %s", prefUtil.getBoolean(Const.Preferences.ANDROID_ENABLED));

                        } catch (JSONException ex) {
                            Sentry.captureException(ex);

                            if(log != null) log.error(TAG, "JSON Error: " + ex.getLocalizedMessage());
                            listener.onConfigError();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Timber.e("Error: %s", error.toString());
                        if(log != null) log.error(TAG, "Config error: " + error.toString());

                        Sentry.captureException(error);

                        // TODO: why??
                        prefUtil.remove(Const.Preferences.ANDROID_ENABLED);
                        prefUtil.remove(Const.Preferences.TIME_INTERVAL_MINS);
                        prefUtil.remove(Const.Preferences.DISTANCE_INTERVAL_METERS);
                        prefUtil.remove(Const.Preferences.BLUEDOT_MODE);
                        prefUtil.remove(Const.Preferences.MIN_SPEED_KPH);
                        prefUtil.remove(Const.Preferences.MAX_SPEED_KPH);
                        prefUtil.remove(Const.Preferences.MONITORED_APPS);
                        prefUtil.remove(Const.Preferences.DISTANCE_FILTER_METERS);

                        listener.onConfigError();
                    }
                });
    }

    private static void addPostRequestToQueue(final Context context, RequestQueue requestQueue, String endpoint, JSONObject request) {
        PreferenceUtil prefUtil = new PreferenceUtil(context);

        final String appSecret = prefUtil.getString(Const.Preferences.APP_SECRET);
        final String baseUrl = prefUtil.getBoolean(Const.Preferences.DEBUG_MODE)?
                Const.Endpoints.API_DEBUG : Const.Endpoints.API_PRODUCTION;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, baseUrl + endpoint, request,
                null, null) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> params = new HashMap<>();
                        params.put(Const.Fields.AUTHORIZATION, appSecret);

                        return params;
                    }
                };

        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(30000, 3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(jsonRequest);
    }

    private static void addGetRequestToQueue(final Context context, RequestQueue requestQueue, String endpoint, String params,
                                      Response.Listener<JSONObject> successListener, Response.ErrorListener errorListener) {
        final String baseUrl = new PreferenceUtil(context).getBoolean(Const.Preferences.DEBUG_MODE)?
                Const.Endpoints.API_DEBUG : Const.Endpoints.API_PRODUCTION;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + endpoint + params,
                null, successListener, errorListener) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> params = new HashMap<>();
                        params.put(Const.Fields.AUTHORIZATION, new PreferenceUtil(context).getString(Const.Preferences.APP_SECRET));

                        return params;
                    }
                };

        requestQueue.add(jsonRequest);
    }
}
