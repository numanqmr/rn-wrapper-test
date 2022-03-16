package com.landmarksid.lo.listeners;

public interface AppStateListener {
    void onBackground();
    void onForeground();
    void onClosed();

    void onStop();
}
