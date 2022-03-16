
package com.landmarksid.rn;

import android.app.Activity;
import android.util.Log;

import com.facebook.react.BuildConfig;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.landmarksid.lo.LandmarksID;
import com.landmarksid.lo.formats.CustomData;
import com.landmarksid.lo.listeners.EventListener;


public class RNLandmarksidSdkModule extends ReactContextBaseJavaModule implements EventListener {

    private final ReactApplicationContext reactContext;

    private static final String TAG = RNLandmarksidSdkModule.class.getSimpleName();
    private static final String API_KEY = "e4f1d1a4-5513-4ee1-941b-d80236ecae5c";

    private static final String APP_ID = "5afd32d956f16b0004daebfc";
    private static final String APP_SECRET = "oOPEhGYECnvYSEFmeHIsA2VPSwP3YGr9LEAMDhk5iGOOuHCx";

    private static final boolean IS_DEBUG_MODE = BuildConfig.DEBUG;

    private LandmarksID landmarksId;
    private Activity mActivity;


    public RNLandmarksidSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.mActivity = getCurrentActivity();
    }

    @ReactMethod
    public void startSDK() {
        CustomData customData = new CustomData();
        customData.addString("country", "de");
        customData.addInt("countryCode", 49);
        customData.addFloat("score", 23.58f);
        customData.addBoolean("uralic", false);

        LandmarksID.Options options = new LandmarksID.Options()
                .setApiKey(API_KEY)
                .setAppMetadata(APP_ID, APP_SECRET)
                .setDebugMode(IS_DEBUG_MODE)
                .setCustomerId("sample-id")
                .setCustomData(customData);

        landmarksId = LandmarksID.getInstance().start(mActivity, options, this);
        Log.d(TAG, "start SDK success: ");
        try {
            landmarksId.setLocationUsable(mActivity);
        } catch (Exception e) {
            Log.e(TAG, "start SDK failure: " + e.getLocalizedMessage());
        }
    }

    @Override
    public String getName() {
        return "RNLandmarksidSdk";
    }

    @Override
    public void msg(String tag, String message) {
        //todo handle messages here
    }

    @Override
    public void success(String tag, String message) {
        //todo handle success here
    }

    @Override
    public void error(String tag, String message) {
        //todo handle errors here

    }

    @Override
    public void onLore(String tag, String time, String message, String type) {
        //todo handle lore updates here
    }

    @Override
    public void onInit(String tag, String message) {
        //todo handle init updates here
    }

    @Override
    public void onConfig(boolean androidEnabled, long timeInterval, long distanceInterval, long minSpeedKph, long maxSpeedKph) {
        //todo handle config updates here
    }
}