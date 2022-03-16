package com.landmarksid.lo.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;

/**
 *
 */
public class PreferenceUtil {
    private SharedPreferences sharedPreferences;

    public PreferenceUtil(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void put(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void put(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void put(String key, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public void put(String key, double value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, (float) value);
        editor.apply();
    }

    public void put(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void put(String key, JSONArray jsonArray) {
        put(key, jsonArray.toString());
    }

    public void remove(String key) {
        if(!sharedPreferences.contains(key)) return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public String getString(String key) {
        return getStringWithDefault(key, "");
    }

    public String getStringWithDefault(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public int getInt(String key) {
        return getIntWithDefault(key, -1);
    }

    public int getIntWithDefault(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public long getLong(String key) {
        return getLongWithDefault(key, -1L);
    }

    public long getLongWithDefault(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public double getDouble(String key) {
        return getDoubleWithDefault(key, 0.0);
    }

    public double getDoubleWithDefault(String key, double defaultValue) {
        return sharedPreferences.getFloat(key, (float) defaultValue);
    }

    public boolean getBoolean(String key) {
        return getBooleanWithDefault(key, false);
    }

    public boolean getBooleanWithDefault(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public boolean hasString(String key) {
        return !getString(key).equals("");
    }
}
