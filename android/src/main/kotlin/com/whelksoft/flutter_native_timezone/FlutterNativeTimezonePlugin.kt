package com.whelksoft.flutter_native_timezone

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import java.time.ZoneId
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterNativeTimezonePlugin: FlutterPlugin, MethodCallHandler {

  private lateinit var channel : MethodChannel

  // backward compatibility with flutter api v1
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val plugin = FlutterNativeTimezonePlugin()
      plugin.setupMethodChannel(registrar.messenger())
    }
  }

  override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    setupMethodChannel(binding.binaryMessenger)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "getLocalTimezone" -> {
        val zone: ZoneId = ZoneId.systemDefault()
        result.success(zone.id)
      }
      "getAvailableTimezones" -> {
        result.success(ZoneId.getAvailableZoneIds().toCollection(ArrayList()))
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun setupMethodChannel(messenger: BinaryMessenger) {
    channel = MethodChannel(messenger, "flutter_native_timezone")
    channel.setMethodCallHandler(this)
  }
}
