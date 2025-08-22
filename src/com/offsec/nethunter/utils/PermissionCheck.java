package com.offsec.nethunter.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionCheck {
    private static final String TAG = "PermissionCheck";
    private final Activity activity;
    private final Context context;
    public static final int DEFAULT_PERMISSION_RQCODE = 1;
    public static final int NH_TERM_PERMISSIONS_RQCODE = 2;

    public static final String[] DEFAULT_PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public static final String[] NH_TERM_PERMISSIONS = {
    };

    public PermissionCheck(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    // 每次应用全新启动时首先检查权限
    public void checkPermissions(String[] PERMISSIONS, int REQUEST_CODE) {
        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_CODE);
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isAllPermitted(String[] PERMISSIONS){
        for (String permissions:PERMISSIONS){
            if (ContextCompat.checkSelfPermission(context, permissions) != PackageManager.PERMISSION_GRANTED){
                Log.e(TAG, "Permissions are NOT all granted.");
                return true;
            }
        }
        Log.d(TAG, "All permissions are granted.");
        return false;
    }

    public void requestPermissions() {
        if (isAllPermitted(DEFAULT_PERMISSIONS)) {
            checkPermissions(DEFAULT_PERMISSIONS, DEFAULT_PERMISSION_RQCODE);
        }
        if (isAllPermitted(NH_TERM_PERMISSIONS)) {
            checkPermissions(NH_TERM_PERMISSIONS, NH_TERM_PERMISSIONS_RQCODE);
        }
    }
}
