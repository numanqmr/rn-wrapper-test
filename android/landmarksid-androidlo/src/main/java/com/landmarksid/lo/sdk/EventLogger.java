package com.landmarksid.lo.sdk;

import com.landmarksid.lo.listeners.EventListener;

public class EventLogger {
    private static EventLogger instance;
    private EventListener eventListener;

    private EventLogger() { }

    public static synchronized EventLogger getInstance() {
        if(instance == null)
            instance = new EventLogger();

        return instance;
    }

    public void setEventListener(EventListener eventListener) {
        instance.eventListener = eventListener;
    }

    public EventListener getEventListener() {
        return instance.eventListener;
    }
}
