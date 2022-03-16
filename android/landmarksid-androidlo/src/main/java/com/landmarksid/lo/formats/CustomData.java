package com.landmarksid.lo.formats;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;

public class CustomData extends HashSet<CustomValue> {
    @Override
    public boolean add(CustomValue v) {
        if(contains(v)) remove(v);
        return super.add(v);
    }

    public void addFloat(String key, float value) {
        add(CustomValue.newFloat(key, value));
    }

    public void addInt(String key, int value) {
        add(CustomValue.newInt(key, value));
    }

    public void addBoolean(String key, boolean value) {
        add(CustomValue.newBoolean(key, value));
    }

    public void addString(String key, String value) {
        add(CustomValue.newString(key, value));
    }

    public JSONArray getJson() throws JSONException {
        JSONArray customDataArr = new JSONArray();
        for(CustomValue cv : this)
            customDataArr.put(cv.getJson());

        return customDataArr;
    }
}
