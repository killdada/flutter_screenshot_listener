package com.mysoft.flutter_screenshot_listener;

import androidx.annotation.NonNull;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterNativeView;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * FlutterScreenshotListenerPlugin
 */
public class FlutterScreenshotListenerPlugin implements MethodCallHandler {
    public static void registerWith(Registrar registrar) {
        new FlutterScreenshotListenerPlugin(registrar);
    }

    private Registrar registrar;

    private MethodChannel methodChannel;

    private PermissionRequester permissionRequester;

    private ScreenshotMgr screenshotMgr;

    private FlutterScreenshotListenerPlugin(Registrar registrar) {
        this.registrar = registrar;

        methodChannel = new MethodChannel(registrar.messenger(), "com.mysoft/screenshot_listener");
        methodChannel.setMethodCallHandler(this);

        permissionRequester = new PermissionRequester(registrar.activity());

        registrar.addRequestPermissionsResultListener(new PluginRegistry.RequestPermissionsResultListener() {
            @Override
            public boolean onRequestPermissionsResult(int id, String[] permissions, int[] grantResults) {
                permissionRequester.onRequestPermissionsResult(id, permissions, grantResults);
                return true;
            }
        });

        registrar.addViewDestroyListener(new PluginRegistry.ViewDestroyListener() {
            @Override
            public boolean onViewDestroy(FlutterNativeView flutterNativeView) {
                if (screenshotMgr != null) {
                    screenshotMgr.unregister();
                }
                return false;
            }
        });
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull final Result result) {
        switch (call.method) {
            case "register":
                permissionRequester.execute(new PermissionRequester.Callback() {
                    @Override
                    public void granted() {
                        if (screenshotMgr == null) {
                            screenshotMgr = new ScreenshotMgr(registrar.context());
                        }
                        screenshotMgr.register(new ScreenshotMgr.Callback() {
                            @Override
                            public void onScreenshot(final String path) {
                                methodChannel.invokeMethod("onScreenshot", path);
                            }
                        });
                    }
                }, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE);
                result.success(null);
                break;
            case "unregister":
                if (screenshotMgr != null) {
                    screenshotMgr.unregister();
                }
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }
}