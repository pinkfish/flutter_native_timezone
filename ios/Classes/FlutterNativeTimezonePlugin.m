#import "FlutterNativeTimezonePlugin.h"

@implementation FlutterNativeTimezonePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"flutter_native_timezone"
            binaryMessenger:[registrar messenger]];
  FlutterNativeTimezonePlugin* instance = [[FlutterNativeTimezonePlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getLocalTimezone" isEqualToString:call.method]) {
    NSTimeZone *timeZone = [NSTimeZone localTimeZone];
    NSString *tzName = [timeZone name];
    result(tzName);
  } else if([@"getAvailableTimezones" isEqualToString:call.method]) {
      result([NSTimeZone knownTimeZoneNames]);
  }
  else {
    result(FlutterMethodNotImplemented);
  }
}

@end
