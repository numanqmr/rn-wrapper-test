package com.landmarksid.lo.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public abstract class PermissionUtil {
    public static void requestPermission(Activity activity, int requestId, String[] permissions) {
        if(activity != null)
            ActivityCompat.requestPermissions(activity, permissions, requestId);
    }

    public static boolean hasAllPermissions(Context context, String[] permissions) {
        for(String permission : permissions)
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;

        return true;
    }

    public static Permission isPermissionGranted(String[] grantPermissions, int[] grantResults, String[] permissions) {
        for(int i = 0; i < grantPermissions.length; i++) {
            for(String permission : permissions)
                if(permission.equals(grantPermissions[i]) && grantResults[i] != PackageManager.PERMISSION_GRANTED)
                    return Permission.DENIED;
        }

        return Permission.GRANTED;
    }
}
