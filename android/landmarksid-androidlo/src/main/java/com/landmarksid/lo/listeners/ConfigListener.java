package com.landmarksid.lo.listeners;

public interface ConfigListener {
    void onDisable();

    void onError();
    void onSuccess(boolean isAndroidEnabled, long timeInterval, long distanceInterval, int discoveryMode, long minSpeedKph,
                   long maxSpeedKph, String monitoredApps, long distanceFilterMeters, int batchSize);
}
