#import "TimezonePlugin.h"

@implementation TimezonePlugin

+ (void)registerWithRegistrar:(NSObject <FlutterPluginRegistrar> *)registrar {
    FlutterMethodChannel *channel = [FlutterMethodChannel
            methodChannelWithName:@"com.whelksoft.timezone"
                  binaryMessenger:[registrar messenger]];
    UIViewController *host = UIApplication.sharedApplication.delegate.window.rootViewController;
    TimezonePlugin *instance = [[TimezonePlugin alloc] initWithHost:host channel:channel];
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
    } else if ([@"showPlacesPicker" isEqualToString:call.method]) {
        result(@YES);
    } else {
        result(FlutterMethodNotImplemented);
    }
}


- (NSArray *)buttonItemsFromActions:(NSArray *)actions {
    NSMutableArray *buttons = [NSMutableArray array];
    if (actions) {
        for (NSDictionary *action in actions) {
            UIBarButtonItem *button = [[UIBarButtonItem alloc] initWithTitle:[action valueForKey:@"title"]
                                                                       style:UIBarButtonItemStylePlain
                                                                      target:self
                                                                      action:@selector(handleToolbar:)];
            button.tag = [[action valueForKey:@"identifier"] intValue];
            [buttons addObject:button];
        }
    }
    return buttons;
}

@end
