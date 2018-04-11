import 'dart:async';
import 'package:flutter/services.dart';


class NativeTimezone {
  static MethodChannel _channel = const MethodChannel("com.whelksoft.timezone");

  static Future<String> getLocalTimezone() async {
    print('getting local timezone');
    dynamic res = await _channel.invokeMethod("getLocalTimezone");
    print(res);
    return res.toString();
  }
}
