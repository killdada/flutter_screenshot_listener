package com.mysoft.flutter_screenshot_listener;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zourw on 2019/4/24.
 */
public class PermissionRequester {
    private Activity activity;
    private Callback callback;

    private static final int REQUEST_CODE = 910820;

    public static abstract class Callback {
        public abstract void granted();

        public void denied(String[] permissions) {
        }

        public void rationale(String[] permissions) {
        }
    }

    public PermissionRequester(Activity activity) {
        this.activity = activity;
    }

    public void execute(Callback callback, String... permissions) {
        this.callback = callback;

        List<String> noPermissions = new ArrayList<>();
        for (String permission : permissions) {
            int status = ActivityCompat.checkSelfPermission(activity, permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                noPermissions.add(permission);
            }
        }

        if (noPermissions.isEmpty()) {
            if (callback != null) {
                callback.granted();
            }
        } else {
            ActivityCompat.requestPermissions(activity, noPermissions.toArray(new String[0]), REQUEST_CODE);
        }
    }

    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE) {
            return;
        }

        final List<String> deniedPermissions = new ArrayList<>();// 拒绝的
        final List<String> rationalePermissions = new ArrayList<>();// 可再次申请的

        for (int i = 0, length = permissions.length; i < length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                String permission = permissions[i];
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    rationalePermissions.add(permission);
                } else {
                    deniedPermissions.add(permission);
                }
            }
        }

        if (!rationalePermissions.isEmpty()) {
            if (callback != null) {
                callback.rationale(rationalePermissions.toArray(new String[0]));
            }
            return;
        }

        if (deniedPermissions.isEmpty()) {
            if (callback != null) {
                callback.granted();
            }
        } else {
            if (callback != null) {
                callback.denied(deniedPermissions.toArray(new String[0]));
            }
        }
    }
}