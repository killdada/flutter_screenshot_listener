package com.mysoft.flutter_screenshot_listener;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by Zourw on 2019/3/6.
 */
public class RealScreenSize {
    public static final Point region = new Point();

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static void init(Context context) {
        if (region.x > 0 && region.y > 0) {
            return;
        }
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return;
        }
        Display display = manager.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(region);
        } else {
            try {
                region.x = (int) Display.class.getMethod("getRawWidth").invoke(display);
                region.y = (int) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception e) {
                display.getSize(region);
            }
        }
    }
}