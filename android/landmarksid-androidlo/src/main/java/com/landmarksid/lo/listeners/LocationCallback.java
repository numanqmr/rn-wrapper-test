package com.landmarksid.lo.listeners;

import android.location.Location;

public interface LocationCallback {
    void onLocationAvailable(Location location);
    void onFailure();
}
