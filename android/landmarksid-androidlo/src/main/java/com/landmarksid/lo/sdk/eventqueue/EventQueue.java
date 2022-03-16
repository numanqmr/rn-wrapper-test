package com.landmarksid.lo.sdk.eventqueue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.landmarksid.lo.Const;
import com.landmarksid.lo.sdk.Api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.LinkedList;

import io.sentry.Sentry;
import timber.log.Timber;

class EventQueue {
    private final static String TAG = "landmarks.eventqueue";
    private final static String FILENAME = "landmarks-eq_db";

    final static int DEFAULT_CAPACITY = 10;

    @SuppressLint("StaticFieldLeak")
    private static EventQueue instance;

    private Context context;
    private LinkedList<JSONObject> queue;

    private EventQueue() {
        queue = new LinkedList<>();
    }

    static synchronized EventQueue getInstance(Context context) {
        if(instance == null)
            instance = new EventQueue();

        if(instance.context == null)
            instance.context = context;

        instance.checkNull();

        return instance;
    }

    private void checkNull() {
        if(queue == null || isEmpty())
            read();

        else if(queue.size() > 0 && queue.get(0) == null)
            read();
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }

    int size() {
        return queue.size();
    }

    void add(JSONObject request) {
        checkNull();

        queue.add(request);
        write();
    }

    JSONObject removeLast() {
        checkNull();
        if(isEmpty()) return null;

        return queue.removeLast();
    }

    JSONObject getFirst() {
        return isEmpty()? null : queue.getFirst();
    }

    JSONObject getLast() {
        return isEmpty()? null : queue.getLast();
    }

    LinkedList<JSONObject> asList() {
        return queue;
    }

    void clear() {
        queue.clear();
        delete();
    }

    private void read() {
        try {
            if(queue == null) queue = new LinkedList<>();
            queue.clear();

            File file = new File(context.getFilesDir(), FILENAME);

            if(file.exists()) {

                FileLock lock = null;
                try {
                    FileInputStream fis = context.openFileInput(FILENAME);
                    //region Lock the event file, no writing on it the file while reading it
                    lock = fis.getChannel().lock(0L, Long.MAX_VALUE, /*shared*/true);
                    InputStreamReader isr = new InputStreamReader(fis);

                    BufferedReader bufferedReader = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null)
                        sb.append(line);

                    JSONArray arr = new JSONObject(sb.toString()).getJSONArray(Const.Fields.EVENTS);

                    for (int i = 0; i < arr.length(); i++) {
                        if (!arr.isNull(i) && !arr.getJSONObject(i).isNull(Const.Fields.LAT)) {
                            queue.add(arr.getJSONObject(i));
                            Timber.d("File: %s found and read: %s", FILENAME, queue);
                        }
                    }
                    lock.release();
                    bufferedReader.close();
                    isr.close();
                    fis.close();
                    lock = null;
                } finally {
                    if(lock != null) {
                        lock.release();
                    }
                    //endregion
                }
            }

        } catch (Exception ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
        }
    }

    private void write() {

        try {
            delete();

            JSONArray arr = new JSONArray();
            for(JSONObject obj : queue) {
                if(obj != null && !obj.isNull(Const.Fields.LAT)) {
                    arr.put(obj);
                }
            }

            JSONObject obj = new JSONObject();
            obj.put(Const.Fields.EVENTS, arr);

            String sq = obj.toString();
            //region Lock the event file, while writing the file(Concurrency issue)
            FileLock lock = null;
            try {
                FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                lock = fos.getChannel().lock();
                fos.write(sq.getBytes());
                lock.release();
                fos.close();
                lock = null;
            } finally {
                if(lock != null) {
                    lock.release();
                }
            }
            //endregion

            Timber.d("File written: %s : %sB", FILENAME, sq.getBytes().length);

        } catch (Exception ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
        }
    }

    private void delete() {
        try {
            boolean result = false;

            File file = new File(context.getFilesDir(), FILENAME);
            if(file.exists())
                result = file.delete();

            Timber.d("File deleted: %s: %s", FILENAME, result);

        } catch (Exception ex) {
            Sentry.captureException(ex);
            ex.printStackTrace();
        }
    }
}
