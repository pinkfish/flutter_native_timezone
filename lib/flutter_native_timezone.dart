import 'dart:async';

import 'package:flutter/services.dart';

class FlutterNativeTimezone {
  static const MethodChannel _channel =
      const MethodChannel('flutter_native_timezone');

  ///Returns local timezone
  static Future<String> getLocalTimezone() async {
    dynamic res = await _channel.invokeMethod("getLocalTimezone");
    return res.toString();
  }
}
