package com.landmarksid.lo.sdk.lore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.landmarksid.lo.listeners.LocationCallback;

import timber.log.Timber;

class SingleShotLocationProvider {

    @SuppressLint("MissingPermission")
    static void requestSingleUpdate(final Context context, final LocationCallback callback) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        Timber.d("Location request received. Firing requestSingleUpdate()");

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
                isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(isGpsEnabled) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            locationManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    callback.onLocationAvailable(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) { }
                @Override
                public void onProviderEnabled(String provider) { }
                @Override
                public void onProviderDisabled(String provider) { }

            }, Looper.getMainLooper());

        } else if(isNetworkEnabled) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);

            locationManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    callback.onLocationAvailable(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) { }
                @Override
                public void onProviderEnabled(String provider) { }
                @Override
                public void onProviderDisabled(String provider) { }

            }, Looper.getMainLooper());

        } else {
            callback.onFailure();
        }
    }
}
