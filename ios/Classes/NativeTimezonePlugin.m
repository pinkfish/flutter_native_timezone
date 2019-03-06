#import "NativeTimezonePlugin.h"

@implementation NativeTimezonePlugin

+ (void)registerWithRegistrar:(NSObject <FlutterPluginRegistrar> *)registrar {
    FlutterMethodChannel *channel = [FlutterMethodChannel
            methodChannelWithName:@"com.whelksoft.nativetimezone"
                  binaryMessenger:[registrar messenger]];
    UIViewController *host = UIApplication.sharedApplication.delegate.window.rootViewController;
    NativeTimezonePlugin *instance = [[NativeTimezonePlugin alloc] initWithHost:host channel:channel];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (id)initWithHost:(UIViewController *)host channel:(FlutterMethodChannel *)channel {
    if (self = [super init]) {
        self.host = host;
        self.channel = channel;
    }
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    if ([@"getLocalTimezone" isEqualToString:call.method]) {
        NSTimeZone *timeZone = [NSTimeZone localTimeZone];
        NSString *tzName = [timeZone name];
        result(tzName);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

@end
