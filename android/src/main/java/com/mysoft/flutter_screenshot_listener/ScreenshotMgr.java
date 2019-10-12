package com.mysoft.flutter_screenshot_listener;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zourw on 2019-08-20.
 */
public class ScreenshotMgr {
    private static final String TAG = "ScreenshotMgr";

    private Context context;
    private Callback callback;

    private MediaObserver internalObserver;
    private MediaObserver externalObserver;

    private long startTime;
    private List<String> hasCallbackPaths = new ArrayList<>();

    public interface Callback {
        void onScreenshot(String path);
    }

    public ScreenshotMgr(Context context) {
        this.context = context;
        RealScreenSize.init(context);

        internalObserver = new MediaObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        externalObserver = new MediaObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    public void register(Callback callback) {
        this.callback = callback;

        startTime = System.currentTimeMillis();
        hasCallbackPaths.clear();

        context.getContentResolver().unregisterContentObserver(internalObserver);
        context.getContentResolver().registerContentObserver(internalObserver.uri, false, internalObserver);

        context.getContentResolver().unregisterContentObserver(externalObserver);
        context.getContentResolver().registerContentObserver(externalObserver.uri, false, externalObserver);
    }

    public void unregister() {
        context.getContentResolver().unregisterContentObserver(internalObserver);
        context.getContentResolver().unregisterContentObserver(externalObserver);

        startTime = 0;
        hasCallbackPaths.clear();
    }

    private class MediaObserver extends ContentObserver {
        private final String[] MEDIA_PROJECTIONS = {
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.WIDTH,
                MediaStore.Images.ImageColumns.HEIGHT,
        };

        private final String[] KEYWORDS = {
                "screenshot", "screen_shot", "screen-shot", "screen shot",
                "screencapture", "screen_capture", "screen-capture", "screen capture",
                "screencap", "screen_cap", "screen-cap", "screen cap", "截屏", "截图",
        };

        private Uri uri;

        private MediaObserver(Uri uri) {
            super(new Handler(Looper.getMainLooper()));
            this.uri = uri;
        }

        @Override
        public void onChange(boolean selfChange) {
            Cursor cursor = null;
            try {
                // 数据改变时查询数据库中最后加入的一条数据
                cursor = context.getContentResolver().query(uri, MEDIA_PROJECTIONS, null, null,
                        MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1");

                if (cursor == null || !cursor.moveToFirst()) {
                    Log.e(TAG, "cursor is empty");
                    return;
                }

                final String data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                final long dateTaken = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN));
                final int width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH));
                final int height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT));

                if (checkDataValid(data, dateTaken, width, height)) {
                    if (hasCallbackPaths.contains(data)) {
                        return;
                    }
                    hasCallbackPaths.add(data);
                    new CheckImageValidTask(data, width, height, callback).execute();
                } else {
                    Log.e(TAG, "path: " + data + ", dateTaken: " + dateTaken + ", width: " + width + ", height: " + height);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        private boolean checkDataValid(String path, long dateTaken, int width, int height) {
            if (TextUtils.isEmpty(path)) {
                return false;
            }

            if (dateTaken < startTime || (System.currentTimeMillis() - dateTaken) > 10 * 1000) {
                return false;
            }

            Point region = RealScreenSize.region;
            if (!((width == region.x && height == region.y) || (height == region.x && width == region.y))) {
                return false;
            }

            path = path.toLowerCase();
            for (String keyWork : KEYWORDS) {
                if (path.contains(keyWork)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class CheckImageValidTask extends AsyncTask<Void, Void, String> {
        private String path;
        private int width, height;
        private Callback callback;

        private int retryCount = 0;

        private CheckImageValidTask(String path, int width, int height, Callback callback) {
            this.path = path;
            this.width = width;
            this.height = height;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return doCheck() ? path : null;
        }

        private boolean doCheck() {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            if (options.outWidth == width && options.outHeight == height) {
                return true;
            }

            if (retryCount == 20) {
                Log.e(TAG, "获取截图失败");
                return false;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            retryCount++;
            return doCheck();
        }

        @Override
        protected void onPostExecute(String path) {
            if (callback != null && !TextUtils.isEmpty(path)) {
                callback.onScreenshot(path);
            } else {
                Log.e(TAG, "获取截图失败");
            }
        }
    }
}
