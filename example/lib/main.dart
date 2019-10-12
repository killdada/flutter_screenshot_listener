import 'package:flutter/material.dart';
import 'package:flutter_screenshot_listener/flutter_screenshot_listener.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  String _path;

  var onScreenshot;

  @override
  void initState() {
    onScreenshot = (path) {
      print('path:$path');
      setState(() {
        _path = path;
      });
    };
    ScreenshotListener.register(onScreenshot);
    WidgetsBinding.instance.addObserver(this);

    super.initState();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    switch (state) {
      case AppLifecycleState.resumed:
        ScreenshotListener.register(onScreenshot);
        break;
      case AppLifecycleState.paused:
        ScreenshotListener.unregister();
        break;
      default:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('PATH: ${_path ?? 'no path'}'),
        ),
      ),
    );
  }
}
