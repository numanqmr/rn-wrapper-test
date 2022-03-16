package com.landmarksid.lo.sdk.analytics;

import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONArray;

import io.sentry.Sentry;
import timber.log.Timber;

class MonitoredAppsUtil {
    private static final String TAG = "landmarks.monitored";

    static JSONArray getInstalledApps(String monitoredApps, final PackageManager packageManager) {
        Timber.d("getInstalledApps get called");
        Timber.d("getInstalledApps is monitoring these apps: %s", monitoredApps);
        try {
            if (!monitoredApps.isEmpty()) {
                JSONArray jsonArray = new JSONArray(monitoredApps);
                JSONArray installedApps = new JSONArray();

                Timber.d("Monitored apps: %s", jsonArray);

                for (int i = 0; i < jsonArray.length(); i++) {
                    String app = jsonArray.getString(i);
                    if (isPackageInstalled(app, packageManager))
                        installedApps.put(app);
                }
                return installedApps;
            } else {
                return new JSONArray();
            }
        } catch (Exception ex) {
            Sentry.captureException(ex);
            Timber.d("getInstalledApps got error %s", ex.getLocalizedMessage());
            return new JSONArray();
        }
    }

    private static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        boolean found = true;

        try {
            packageManager.getPackageInfo(packageName, 0);

        } catch (PackageManager.NameNotFoundException e) {
            found = false;
        }

        return found;
    }
}
