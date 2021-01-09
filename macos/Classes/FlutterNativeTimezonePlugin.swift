import Cocoa
import FlutterMacOS

public class FlutterNativeTimezonePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_native_timezone", binaryMessenger: registrar.messenger)
    let instance = FlutterNativeTimezonePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getLocalTimezone":
      result(NSTimeZone.local.identifier)
    case "getAvailableTimezones":
      result(NSTimeZone.knownTimeZoneNames)
    default:
      result(FlutterMethodNotImplemented)
    }
  }
}
