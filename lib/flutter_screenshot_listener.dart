import 'package:flutter/services.dart';

final MethodChannel _channel =
    const MethodChannel('com.mysoft/screenshot_listener');

class ScreenshotListener {
  static register(Function(String path) onScreenshot) async {
    assert(onScreenshot != null);

    _channel.setMethodCallHandler((MethodCall call) {
      if ("onScreenshot" == call.method) {
        onScreenshot(call.arguments);
      }
      return null;
    });

    await _channel.invokeMethod("register");
  }

  static unregister() async {
    _channel.setMethodCallHandler(null);
    await _channel.invokeMethod("unregister");
  }
}
