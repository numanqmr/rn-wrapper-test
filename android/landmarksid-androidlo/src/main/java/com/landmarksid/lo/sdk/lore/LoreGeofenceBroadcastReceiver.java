package com.landmarksid.lo.sdk.lore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.landmarksid.lo.Const;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.analytics.ActivityRecognitionListener;

import timber.log.Timber;

public class LoreGeofenceBroadcastReceiver extends BroadcastReceiver {

    private PreferenceUtil prefUtil;
    private static final String ONE_TIME_GEOFENCE_ON_BROACAST_RECEIVER_LORE_WORKER = "ONE_TIME_GEOFENCE_ON_BROACAST_RECEIVER_LORE_WORKER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.d("Broadcast received: %s", intent);

        this.prefUtil = new PreferenceUtil(context);

        LoreGeofenceWorker.prepareWork(context, intent);
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(LoreGeofenceWorker.class).build();

        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_GEOFENCE_ON_BROACAST_RECEIVER_LORE_WORKER,
                ExistingWorkPolicy.KEEP, workRequest);

        // motion activity recognition e.g. run, walk
        boolean enableMotionActivity = prefUtil.getBoolean(Const.Preferences.ENABLE_MOTION_ACTIVITY);

        //if  motion activity is enabled
        if (enableMotionActivity) {
            ActivityRecognitionListener activityRecognitionListener = new ActivityRecognitionListener(context, intent);
            activityRecognitionListener.build();
        }

    }
}
