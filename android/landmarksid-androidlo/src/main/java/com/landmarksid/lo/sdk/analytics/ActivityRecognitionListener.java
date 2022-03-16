package com.landmarksid.lo.sdk.analytics;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.preferences.PreferenceUtil;

import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ActivityRecognitionListener {

    private  final String TAG = "landmarks.activityType";

    private Context context;
    private Intent intent;
    private PendingIntent pendingIntent;
    private PreferenceUtil prefUtil;

    public ActivityRecognitionListener(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
        this.prefUtil = new PreferenceUtil(context);
    }


    public void build() {


        Boolean alreadyRegisteredMotionActivity = prefUtil.getBoolean(Const.Preferences.ALREADY_REGISTERED_MOTION_ACTIVITY);

        if(alreadyRegisteredMotionActivity == true) {
            // go through activity types and set last type
            this.setActivityType();

        }  else {
            // check the condition to register a activity recognition
            Boolean enableMotionActivity = prefUtil.getBoolean(Const.Preferences.ENABLE_MOTION_ACTIVITY);

            if (enableMotionActivity == true) {

                // set flag for motion activity registry
                prefUtil.put(Const.Preferences.ALREADY_REGISTERED_MOTION_ACTIVITY, true);

                pendingIntent  = PendingIntent.getService( context, 0, this.intent, PendingIntent.FLAG_UPDATE_CURRENT );

                // check if motion activity not registered
                String motionActivity = prefUtil.getString(Const.Preferences.MOTION_ACTIVITY);
                if (motionActivity.equals("")) {
                    // register motion activity
                    this.registerActivityRecognition(this.pendingIntent);
                }

            } else {
                // deregister motion activity
                String motionActivity = prefUtil.getString(Const.Preferences.MOTION_ACTIVITY);
                // reset already registered motion activity flag
                prefUtil.put(Const.Preferences.ALREADY_REGISTERED_MOTION_ACTIVITY, false);

                // deregister when motion activity is not empty
                if (!motionActivity.equals("")) {
                    // make motion activity empty
                    prefUtil.put(Const.Preferences.MOTION_ACTIVITY, "");

                    if(pendingIntent == null) {
                        this.pendingIntent = PendingIntent.getService( context, 0, this.intent, PendingIntent.FLAG_UPDATE_CURRENT );
                    }

                    // deregister
                    this.deRegisterActivityRecognition(this.pendingIntent);
                }
            }

        }

    }

    private  void setActivityType() {

        // process activity transition events
        if (ActivityTransitionResult.hasResult(this.intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                if(event.getActivityType() == DetectedActivity.WALKING) {
                    prefUtil.put(Const.Preferences.MOTION_ACTIVITY, DetectedActivity.WALKING);
                    break;
                } else if(event.getActivityType() == DetectedActivity.IN_VEHICLE) {
                    prefUtil.put(Const.Preferences.MOTION_ACTIVITY, DetectedActivity.IN_VEHICLE);
                    break;
                } else if(event.getActivityType() == DetectedActivity.RUNNING) {
                    prefUtil.put(Const.Preferences.MOTION_ACTIVITY, DetectedActivity.RUNNING);
                    break;
                } else if(event.getActivityType() == DetectedActivity.ON_FOOT) {
                    prefUtil.put(Const.Preferences.MOTION_ACTIVITY, DetectedActivity.ON_FOOT);
                    break;
                } else if(event.getActivityType() == DetectedActivity.ON_BICYCLE) {
                    prefUtil.put(Const.Preferences.MOTION_ACTIVITY, DetectedActivity.ON_BICYCLE);
                    break;
                }
            }
        }
    }

    private void registerActivityRecognition(PendingIntent pendingIntent) {

        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());



        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Task<Void> task = ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Timber.d("Activity type recognition listener is registered successfully");
                    }
                }
        );

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Timber.d("Activity type recognition listener registration is failed: %s", e.getLocalizedMessage());
                    }
                });
    }

    private void deRegisterActivityRecognition(final PendingIntent pendingIntent) {

        Task<Void> task = ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        pendingIntent.cancel();
                    }
                }
        );

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Timber.e("Activity type recognition listener De-registration is failed: %s", e.getMessage());
                    }
                }
        );
    }
}
