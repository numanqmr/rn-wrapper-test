package com.landmarksid.lo.formats;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.sentry.Sentry;
import timber.log.Timber;

public class DateTimeUtil {

    public static String format(long timeInMillis) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timeInMillis);

            return df.format(cal.getTime());

        } catch (Exception ex) {
            Sentry.captureException(ex);
            return "";
        }
    }

    public static long toSeconds(String dateTime) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            return df.parse(dateTime).getTime() / 1000L;

        } catch (Exception ex) {
            Sentry.captureException(ex);
            return 0;
        }
    }

    public static String getCurrentDateTime() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        String currentDateTime = df.format(date);
        return currentDateTime;
    }

    public static Date getDateTime(String dateTime) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        try {
            Date date = df.parse(dateTime);
            return date;
        } catch (ParseException ex) {
            Timber.d(ex.getLocalizedMessage());
        }

        return new Date();
    }

    public static int compareMins(Date first, Date second) {
        long mills =  first.getTime() - second.getTime();
        int mins = (int) (mills/(1000*60)) % 60;
        return Math.abs(mins);
    }
}
