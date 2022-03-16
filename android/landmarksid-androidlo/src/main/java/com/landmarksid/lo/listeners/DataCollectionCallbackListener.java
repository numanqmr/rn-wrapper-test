package com.landmarksid.lo.listeners;

public interface DataCollectionCallbackListener {
    void onConfigReceived(boolean isAndroidEnabled);
    void onConfigError();
}
