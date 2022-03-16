package com.landmarksid.lo.formats;

import com.landmarksid.lo.Const;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class CustomValue implements Serializable {
    private String key;
    private String value;
    private String type;

    private CustomValue(String key, String value, String type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    static CustomValue newFloat(String key, float value) {
        return new CustomValue(key, String.valueOf(value), "float");
    }

    static CustomValue newInt(String key, int value) {
        return new CustomValue(key, String.valueOf(value), "int");
    }

    static CustomValue newBoolean(String key, boolean value) {
        return new CustomValue(key, String.valueOf(value), "bool");
    }

    static CustomValue newString(String key, String value) {
        return new CustomValue(key, value, "string");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(Const.Fields.KEY, key);
        obj.put(Const.Fields.VALUE, value);
        obj.put(Const.Fields.TYPE, type);

        return obj;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof CustomValue) && ((CustomValue) o).key.equals(this.key);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }
}
