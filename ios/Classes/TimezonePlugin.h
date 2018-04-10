#import <Flutter/Flutter.h>
#import <CoreLocation/CoreLocation.h>
@class MapViewController;

@class GMSCameraPosition;

@interface TimezonePlugin : NSObject<FlutterPlugin>

@property (nonatomic, assign) UIViewController *host;
@property (nonatomic, assign) FlutterMethodChannel *channel;
@end
