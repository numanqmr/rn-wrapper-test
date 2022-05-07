
package com.landmarksid.rn;

import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.landmarksid.lo.core.LandmarksID;
import com.landmarksid.lo.logging.EventLogListener;
import com.landmarksid.lo.types.CustomData;
import com.landmarksid.rn.BuildConfig;

import org.jetbrains.annotations.NotNull;

import static com.landmarksid.rn.Const.ERROR;
import static com.landmarksid.rn.Const.MESSAGE;
import static com.landmarksid.rn.Const.ON_CONFIG;
import static com.landmarksid.rn.Const.ON_INIT;
import static com.landmarksid.rn.Const.ON_LORE;
import static com.landmarksid.rn.Const.RN_LANDMARKSID_SDK;
import static com.landmarksid.rn.Const.SUCCESS;

public class RNLandmarksidSdkModule extends ReactContextBaseJavaModule implements LifecycleEventListener, EventLogListener {

    private static final String TAG = RNLandmarksidSdkModule.class.getSimpleName();


    private final ReactApplicationContext reactContext;
    private static LandmarksID landmarksId;

    //TODO : secure the keys in .properties file for safety.
    private static final String API_KEY = "755a32cf-00fd-4b85-8787-ce177b93d8a0";
    private static final String APP_ID = "5df70cc3919e370004a297c2";
    private static final String APP_SECRET = "97pxpfkzFi1Bn9F5TeIrotJsiq3KVPS3AzLNxPPWLwHy3ihO";

    private static final boolean IS_DEBUG_MODE = BuildConfig.DEBUG;

    private CustomData customData = new CustomData();

    public RNLandmarksidSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @ReactMethod
    public void startSDK() {

        LandmarksID.Options options = new LandmarksID.Options()
                .setApiKey(API_KEY)
                .setAppMetadata(APP_ID, APP_SECRET)
                .setDebugMode(IS_DEBUG_MODE)
                .setCustomerId("sample-id")// TODO add actual customer Id here.
                .setCustomData(getDefaultData()); // TODO remove this when you are using your custom data

        landmarksId = LandmarksID.getInstance().start(getCurrentActivity(), options, this);
    }

    @ReactMethod
    public void stopSDK(){
        if (landmarksId != null)
            landmarksId.stop(getCurrentActivity());
    }

    @ReactMethod
    public void setCustomData(String name, String value) {
        customData.add(name, value);
        landmarksId.setCustomData(customData);
    }

    @ReactMethod
    public void setCustomData(String name, Float value) {
        customData.add(name, value);
        landmarksId.setCustomData(customData);
    }

    @ReactMethod
    public void setCustomData(String name, Integer value) {
        customData.add(name, value);
        landmarksId.setCustomData(customData);
    }

    @ReactMethod
    public void setCustomData(String name, Boolean value) {
        customData.add(name, value);
        landmarksId.setCustomData(customData);
    }

    // Required for rn built in EventEmitter Calls else gives warning in console
    @ReactMethod
    public void addListener(String eventName) {

    }
    // Required for rn built in EventEmitter Calls else gives warning in console
    @ReactMethod
    public void removeListeners(Integer count) {

    }

    private CustomData getDefaultData(){
        CustomData customData = new CustomData();
        customData.add("country", "de");
        customData.add("countryCode", 49);
        customData.add("score", 23.58f);
        customData.add("uralic", false);
        return customData;
    }

    @Override
    public @NotNull String getName() {
        return RN_LANDMARKSID_SDK;
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {

    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    /**
     * Here we'll setup native events that can be subscribed in react-native code for android
     * to receive live events.
     */
    @Override
    public void msg(String tag, String message) {
        Log.i(TAG, "msg: " + "tag:" + tag + "message:" + message);

        WritableMap params = Arguments.createMap();
        params.putString("tag", tag);
        params.putString("message", message);

        sendEvent(reactContext, MESSAGE, params);
    }

    @Override
    public void success(String tag, String message) {
        Log.i(TAG, "success: " + "tag:" + tag + "message:" + message);
        WritableMap params = Arguments.createMap();
        params.putString("tag", tag);
        params.putString("message", message);

        sendEvent(reactContext, SUCCESS, params);

    }

    @Override
    public void error(String tag, String message) {
        Log.e(TAG, "error: " + "tag:" + tag + "message:" + message);
        WritableMap params = Arguments.createMap();
        params.putString("tag", tag);
        params.putString("message", message);

        sendEvent(reactContext, ERROR, params);


    }

    @Override
    public void onLore(String tag, String time, String message, String type) {
        Log.i(TAG, "onLore: " + "tag:" + tag + "message:" + message);
        WritableMap params = Arguments.createMap();
        params.putString("tag", tag);
        params.putString("time", time);
        params.putString("message", message);
        params.putString("type", type);

        sendEvent(reactContext, ON_LORE, params);

    }

    @Override
    public void onInit(String tag, String message) {
        Log.i(TAG, "onInIt: " + "tag:" + tag + "message:" + message);
        WritableMap params = Arguments.createMap();
        params.putString("tag", tag);
        params.putString("message", message);

        sendEvent(reactContext, ON_INIT, params);

    }

    @Override
    public void onConfig(final boolean androidEnabled, final long timeInterval, final long distanceInterval, final long minSpeedKph, final long maxSpeedKph) {
        WritableMap params = Arguments.createMap();
        params.putBoolean("androidEnabled", androidEnabled);
        params.putDouble("timeInterval", timeInterval);
        params.putDouble("distanceInterval", distanceInterval);
        params.putDouble("minSpeedKph", minSpeedKph);
        params.putDouble("maxSpeedKph", maxSpeedKph);

        sendEvent(reactContext, ON_CONFIG, params);

    }
}