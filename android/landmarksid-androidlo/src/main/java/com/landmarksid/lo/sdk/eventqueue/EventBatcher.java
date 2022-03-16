package com.landmarksid.lo.sdk.eventqueue;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.landmarksid.lo.Const;
import com.landmarksid.lo.formats.DateTimeUtil;
import com.landmarksid.lo.listeners.EventListener;
import com.landmarksid.lo.preferences.PreferenceUtil;
import com.landmarksid.lo.sdk.Api;
import com.landmarksid.lo.sdk.EventLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.sentry.Sentry;
import timber.log.Timber;

public class EventBatcher {
    private static final String TAG = "landmarks.eventbatcher";

    private static final int ADD_NONE = 0;
    private static final int ADD_NEW = 1;
    private static final int ADD_TO_EXISTING = 2;

    @SuppressWarnings("StaticFieldLeak")
    private static volatile EventBatcher instance;

    private PreferenceUtil prefUtil;
    private Context context;

    private RequestQueue requestQueue;
    private int capacity;

    private EventListener log;

    private EventBatcher() {
        if(instance != null)
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
    }

    public static EventBatcher getInstance(Context context) {
        if(instance == null) {
            synchronized (EventBatcher.class) {
                if(instance == null) {
                    instance = new EventBatcher();
                    instance.setCapacity(new PreferenceUtil(context).getIntWithDefault(Const.Preferences.BATCH_SIZE,
                            EventQueue.DEFAULT_CAPACITY));

                    if(instance.requestQueue == null)
                        instance.requestQueue = instance.getRequestQueue(context);

                    if(instance.prefUtil == null)
                        instance.prefUtil = new PreferenceUtil(context);

                    if(instance.context == null)
                        instance.context = context;

                    instance.log = EventLogger.getInstance().getEventListener();
                }
            }
        }

        return instance;
    }

    private RequestQueue getRequestQueue(Context context) {
        if(requestQueue == null)
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());

        return requestQueue;
    }

    private void setCapacity(int capacity) {
        this.capacity = capacity;
        log("Event queue capacity set: " + capacity);
    }

    public void flush() {
        log("Flushing event queue...");
        if(!EventQueue.getInstance(context).isEmpty()) dispatchEvents();
    }

    public void stop() {
        log("Stopping event batcher...");

        flush();
        instance = null;
    }

    public void addEventToQueue(JSONObject request) {
        EventQueue queue = EventQueue.getInstance(context);

        switch(getAddMethod(request)) {
            case ADD_NONE:
                log("Either too soon or too close to previous LORE. Not adding to queue");
                break;

            case ADD_NEW:
                try {
                    queue.add(request);
                    log("LORE added to queue as new event. Events in queue: " + queue.size() + " Capacity: " + capacity);

                } catch (Exception ex) {
                    Sentry.captureException(ex);
                }
                break;

            case ADD_TO_EXISTING:
                try {
                    JSONObject last = queue.removeLast();
                    JSONArray sourceEvents;
                    if(last.has(Const.Fields.SOURCE_EVENTS)) {
                        sourceEvents = last.getJSONArray(Const.Fields.SOURCE_EVENTS);

                    } else {
                        sourceEvents = new JSONArray();
                        sourceEvents.put(toAggregatedObject(last));
                    }

                    sourceEvents.put(toAggregatedObject(request));
                    request.put(Const.Fields.SOURCE_EVENTS, sourceEvents);

                    queue.add(request);
                    log("LORE added as an aggregated event to last event in queue");

                } catch (JSONException ex) {
                    Sentry.captureException(ex);
                    log("JSONException while creating aggregated event: " + ex.getLocalizedMessage());
                }

                break;
        }

        Timber.d("Queue: %s", queue.asList());

        try {
            // TODO: why double check?
            if(!queue.isEmpty() && queue.getFirst() != null) {
                long timeFirst = DateTimeUtil.toSeconds(queue.getFirst().has(Const.Fields.SOURCE_EVENTS) ?
                        queue.getFirst().getJSONArray(Const.Fields.SOURCE_EVENTS).getJSONObject(0).getString(Const.Fields.EVENT_TIME)
                        : queue.getFirst().getString(Const.Fields.EVENT_TIME));

                if ((System.currentTimeMillis() / 1000) - timeFirst >= 12 * 60 * 60) {
                    log("More than 12 hours passed. Flushing queue");
                    dispatchEvents();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if(queue.size() == capacity)
            dispatchEvents();
    }

    private JSONObject toAggregatedObject(JSONObject object) throws JSONException {
        JSONObject res = new JSONObject();
        res.put(Const.Fields.EVENT_TIME, object.getString(Const.Fields.EVENT_TIME));
        res.put(Const.Fields.LAT, object.getDouble(Const.Fields.LAT));
        res.put(Const.Fields.LONG, object.getDouble(Const.Fields.LONG));
        res.put(Const.Fields.ALTITUDE, object.getDouble(Const.Fields.ALTITUDE));
        res.put(Const.Fields.HORIZONTAL_ACCURACY, object.getDouble(Const.Fields.HORIZONTAL_ACCURACY));
        res.put(Const.Fields.VERTICAL_ACCURACY, object.getDouble(Const.Fields.VERTICAL_ACCURACY));
        res.put(Const.Fields.EVENT_TRIGGER, object.getString(Const.Fields.EVENT_TRIGGER));

        return res;
    }

    private void dispatchEvents() {
        log("Queue capacity reached or it has been flushed: " + EventQueue.getInstance(context).size() + ", Capacity: " + capacity);
        EventQueue queue = EventQueue.getInstance(context);

        try {
            JSONArray eventsArr = new JSONArray();
            for(JSONObject obj : queue.asList())
                eventsArr.put(obj);

            JSONObject request = new JSONObject();
            request.put(Const.Fields.EVENTS, eventsArr);

            Api.postLOEvents(context.getApplicationContext(), requestQueue, request);

        } catch(JSONException ex) {
            Sentry.captureException(ex);
            if(log != null) log.error(TAG, "JSONException while dispatching LO events: " + ex.getLocalizedMessage());
        }

        queue.clear();
    }

    private int getAddMethod(JSONObject current) {
        EventQueue queue = EventQueue.getInstance(context);
        if(queue.isEmpty()) return ADD_NEW;

        try {
            JSONArray sourceEvents = null;
            if(queue.getLast().has(Const.Fields.SOURCE_EVENTS))
                sourceEvents = queue.getLast().getJSONArray(Const.Fields.SOURCE_EVENTS);

            long curTime = DateTimeUtil.toSeconds(current.getString(Const.Fields.EVENT_TIME));
            long prevTime = DateTimeUtil.toSeconds(sourceEvents == null? queue.getLast().getString(Const.Fields.EVENT_TIME)
                    : sourceEvents.getJSONObject(0).getString(Const.Fields.EVENT_TIME));
            long lastTime = DateTimeUtil.toSeconds(queue.getLast().getString(Const.Fields.EVENT_TIME));

            Location curLoc = toLocation(current);
            Location prevLoc = toLocation(sourceEvents == null? queue.getLast() : sourceEvents.getJSONObject(0));
            Location lastLoc = toLocation(queue.getLast());

            long timeDiff = curTime - prevTime; // Seconds
            long timeDiffLast = curTime - lastTime; // Seconds

            double distDiff = prevLoc.distanceTo(curLoc); // Meters
            double distDiffLast = lastLoc.distanceTo(curLoc); // Meters

            boolean isTimeOk = timeDiffLast >= prefUtil.getLong(Const.Preferences.TIME_INTERVAL_MINS)*30;
            boolean isDistOk = distDiffLast >= (double) prefUtil.getLong(Const.Preferences.DISTANCE_INTERVAL_METERS)/2;

            Timber.d("Time Diff: %s, %s Dist Diff: %s, %s Distance Filter: %s",
                    timeDiff, timeDiffLast, distDiff, distDiffLast, prefUtil.getLong(Const.Preferences.DISTANCE_FILTER_METERS));

            if(isTimeOk || isDistOk) {
                return ((timeDiff > 2 * 60 * 60) || (distDiff > prefUtil.getLong(Const.Preferences.DISTANCE_FILTER_METERS))) ?
                        ADD_NEW : ADD_TO_EXISTING;

            } else return ADD_NONE;

        } catch (Exception ex) {
            Sentry.captureException(ex);
            return ADD_NONE;
        }
    }

    private Location toLocation(JSONObject jsonObject) throws JSONException {
        Location location = new Location("Loc" + System.currentTimeMillis());
        location.setLatitude(jsonObject.getDouble(Const.Fields.LAT));
        location.setLongitude(jsonObject.getDouble(Const.Fields.LONG));

        return location;
    }

    private void log(String msg) {
        if(log != null) log.msg(TAG, msg);
        Timber.d(msg);
    }
}
