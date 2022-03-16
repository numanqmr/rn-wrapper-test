package com.landmarksid.lo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.landmarksid.lo.listeners.AppStateListener;
import com.landmarksid.lo.listeners.ConfigListener;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.LandmarksSDKWorker;
import com.landmarksid.lo.sdk.Utils;
import com.landmarksid.lo.sdk.lore.LoreWorker;

import java.util.List;

import timber.log.Timber;

public class LandmarksIDManager implements Application.ActivityLifecycleCallbacks {
    private static String TAG = "landmarks.manager";

    private static final String ONE_TIME_LANDMARKS_ID_MANAGER_LORE_WORKER = "ONE_TIME_LANDMARKS_ID_MANAGER_LORE_WORKER";

    private static LandmarksIDManager instance;


    private AppStateListener appStateListener;
    private boolean inForeground;

    /*flag app launch, stays false until the first time the app goes to background*/
    private boolean freshStart = true;

    private long lastConfigCheck = -1;

    private ConfigListener configListener;

    private LandmarksIDManager(Context context) {
        ((Application) context).registerActivityLifecycleCallbacks(this);
    }

    public static synchronized LandmarksIDManager getInstance(Context context) {
        if(instance == null)
            instance = new LandmarksIDManager(context.getApplicationContext());

        return instance;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        Timber.d("onCreate()");
        //this is disabled to make sure that checkConfig() and checkAnalytics() are not run on before getConfig()
        //setState(true);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Timber.d("onStart()");
        setState(true);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Timber.d("onResume()");
        setState(true);

        // Workaround based on Google's Kotlin version. See: https://issuetracker.google.com/issues/110237673 and 113122354
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = ((ActivityManager) activity.getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses();

        if(runningAppProcesses != null) {
            //TODO: runningAppProcesses.get(0) should not work, please verify with the link above
            // we need to filter and find
            int importance = runningAppProcesses.get(0).importance;
            if(importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                PreferenceUtil preferenceUtil = new PreferenceUtil(activity.getApplicationContext());
                if(appStateListener == null && preferenceUtil.getBooleanWithDefault(Const.Preferences.ANDROID_ENABLED, true)) { // First run

                    Timber.d("Single LORE at onResume()");

                    if(Utils.suppressCheckConfigMins(preferenceUtil, TAG ) == false) {
                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(LoreWorker.class).build();
                        WorkManager.getInstance(activity.getApplicationContext()).enqueueUniqueWork(ONE_TIME_LANDMARKS_ID_MANAGER_LORE_WORKER,
                                ExistingWorkPolicy.KEEP, workRequest);
                    }
                }
            }
        }
    }


    @Override
    public void onActivityPaused(Activity activity) {
        setState(false);
        if(appStateListener != null)
            appStateListener.onClosed();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        setState(false);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        setState(false);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) { }

    private void setState(boolean inForeground) {
        this.inForeground = inForeground;

        if(appStateListener != null) {
            if(inForeground) {
                appStateListener.onForeground();

            } else {
                freshStart = false;
                appStateListener.onBackground();
            }
        }
    }

    public void setAppStateListener(AppStateListener appStateListener) {
        instance.appStateListener = appStateListener;
    }

    // TODO: Should be private or removed
    public void setFreshStart() {
        instance.freshStart = true;
    }

    public boolean isFreshStart() {
        return instance.freshStart;
    }

    public boolean isInForeground() {
        return inForeground;
    }

    /**
     * KILLS all the workers
     * @param context
     */
    public void stopSDK(Context context) {
        if(appStateListener != null) appStateListener.onStop();

        WorkManager.getInstance(context).cancelUniqueWork(LandmarksSDKWorker.ONE_TIME_LOCATION_FOREGROUND_LORE_WORKER);
        WorkManager.getInstance(context).cancelUniqueWork(LandmarksSDKWorker.PERIODIC_RECURRING_PING_LORE_WORKER);

        Timber.d("All workers stopped and listeners notified");
    }

    public void setConfigListener(ConfigListener configListener) {
        instance.configListener = configListener;
    }

    public ConfigListener getConfigListener() {
        return instance.configListener;
    }

    //TODO: to be removed
    public void setLastConfigCheck(long timeInMillis) {
        instance.lastConfigCheck = timeInMillis;
    }

    public long getLastConfigCheck() {
        return instance.lastConfigCheck;
    }
}
