import 'dart:async';

import 'package:flutter/services.dart';

class FlutterNativeTimezone {
  static const MethodChannel _channel =
      const MethodChannel('flutter_native_timezone');

  /// Returns local timezone
  static Future<String> getLocalTimezone() async {
    final String localTimezone =
        await _channel.invokeMethod("getLocalTimezone");
    return localTimezone;
  }

  static Future<List<String>> getAvailableTimezones() async {
    final List<String> availableTimezones =
        await _channel.invokeListMethod<String>("getAvailableTimezones");
    return availableTimezones;
  }
}
