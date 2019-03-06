package com.whelksoft.nativetimezone

import android.app.Activity
import android.content.Intent
import android.location.Location
import java.util.TimeZone
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener

class NativeTimezonePlugin(val activity: Activity) : MethodCallHandler {
    companion object {
        lateinit var channel: MethodChannel


        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            channel = MethodChannel(registrar.messenger(), "com.whelksoft.nativetimezone")
            val plugin = NativeTimezonePlugin(activity = registrar.activity())
            channel.setMethodCallHandler(plugin)
        }
     }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method == "getLocalTimezone" -> {
                result.success(TimeZone.getDefault().getID())
                return
            }
            else -> result.notImplemented()
        }
    }
}