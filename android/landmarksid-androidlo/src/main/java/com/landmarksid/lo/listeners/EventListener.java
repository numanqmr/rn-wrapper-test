package com.landmarksid.lo.listeners;

public interface EventListener {
    void msg(String tag, String message);
    void success(String tag, String message);
    void error(String tag, String message);

    void onLore(String tag, String time, String message, String type);

    void onInit(String tag, String message);
    void onConfig(boolean androidEnabled, long timeInterval, long distanceInterval, long minSpeedKph, long maxSpeedKph);
}
