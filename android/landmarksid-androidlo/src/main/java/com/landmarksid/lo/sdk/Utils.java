package com.landmarksid.lo.sdk;

import android.util.Log;

import com.landmarksid.lo.Const;
import com.landmarksid.lo.formats.DateTimeUtil;
import com.landmarksid.lo.preferences.PreferenceUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import timber.log.Timber;

public class Utils {

    /**
     * Get current datetime and compare with last store date time if less than 60min return false
     * @param prefUtil
     * @param tag
     * @return
     */
    static public boolean suppressCheckConfigMins(PreferenceUtil prefUtil, String tag) {

        //get current datetime and compare with last store date time if less than 60min doesnt do anything
        //LAST_FOREGROUND_CALL

        String lastForegroundCallDateTime = prefUtil.getString(Const.Preferences.LAST_FOREGROUND_CALL_DATE_TIME);
        Boolean suppressCheckConfigMins = true;
        int allowanceMinToRunForeground = prefUtil.getInt(Const.Preferences.SUPPRESS_CHECK_CONFIG_MINS);;


        if (lastForegroundCallDateTime.isEmpty()) {
            DateTimeUtil.getCurrentDateTime();
            prefUtil.put(Const.Preferences.LAST_FOREGROUND_CALL_DATE_TIME, DateTimeUtil.getCurrentDateTime());
            suppressCheckConfigMins = false;
        } else {
            int diffMins = DateTimeUtil.compareMins(DateTimeUtil.getDateTime(DateTimeUtil.getCurrentDateTime()), DateTimeUtil.getDateTime(lastForegroundCallDateTime));
            Timber.d("last foreground call DateTime%s", lastForegroundCallDateTime);
            Timber.d("minutes difference%s", diffMins);
            if (diffMins >= allowanceMinToRunForeground) {
                suppressCheckConfigMins = false;
                prefUtil.put(Const.Preferences.LAST_FOREGROUND_CALL_DATE_TIME, DateTimeUtil.getCurrentDateTime());
            }
            Timber.d("should Run Foreground? %s", suppressCheckConfigMins);
        }

        if (allowanceMinToRunForeground == 0) {
            suppressCheckConfigMins = false;
        }

        return suppressCheckConfigMins;
    }

    static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    static String urlEncodeUTF8(Map<?,?> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<?,?> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s",
                    urlEncodeUTF8(entry.getKey().toString()),
                    urlEncodeUTF8(entry.getValue().toString())
            ));
        }
        return sb.toString();
    }
}
