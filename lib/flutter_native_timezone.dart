import 'dart:async';
import 'package:flutter/services.dart';

class FlutterNativeTimezone {
  static MethodChannel _channel =
      const MethodChannel("com.whelksoft.nativetimezone");

  ///Returns local timezone
  static Future<String> getLocalTimezone() async {
    dynamic res = await _channel.invokeMethod("getLocalTimezone");
    return res.toString();
  }
}
