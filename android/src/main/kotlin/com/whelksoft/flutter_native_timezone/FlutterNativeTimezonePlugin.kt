package com.whelksoft.flutter_native_timezone

import java.util.TimeZone
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterNativeTimezonePlugin: MethodCallHandler {
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "flutter_native_timezone")
      channel.setMethodCallHandler(FlutterNativeTimezonePlugin())
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
        "getLocalTimezone" -> {
          result.success(TimeZone.getDefault().id)
        }
        "getAvailableTimezones" -> {
          result.success(TimeZone.getAvailableIDs().toCollection(ArrayList()))
        }
        else -> {
          result.notImplemented()
        }
    }
  }
}
