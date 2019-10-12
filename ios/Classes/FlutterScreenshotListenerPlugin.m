#import "FlutterScreenshotListenerPlugin.h"

@implementation UIView (MUtilsScreenshot)

- (UIImage *)screenshot
{
    return [self screenshotWithRect:[UIScreen mainScreen].bounds];
}

- (UIImage *)screenshotWithRect:(CGRect)rect;
{
    UIImage * image[2];
    for (int i = 0; i < 2; i++) {
        if (i == 0) {
            // 获得状态栏view的上下文以绘制图片
            UIView *statusBarView = [[UIApplication sharedApplication] valueForKey:@"_statusBar"];
            UIGraphicsBeginImageContextWithOptions(statusBarView.frame.size,NO,[UIScreen mainScreen].scale);
            [statusBarView.layer renderInContext:UIGraphicsGetCurrentContext()];
            image[i] = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
        } else {
            // 获得其他所有window，包括键盘，的上下文并绘制图片
            UIGraphicsBeginImageContextWithOptions(rect.size,NO,[UIScreen mainScreen].scale);

            for (UIWindow *window in [UIApplication sharedApplication].windows) {
                if (![window respondsToSelector:@selector(screen)] || window.screen == [UIScreen mainScreen]) {
                    [window drawViewHierarchyInRect:window.bounds afterScreenUpdates:YES];
                }
            }
            image[i] = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
        }
    }
    // 将上面得到的两张图片合并绘制为一张图片，最终得到screenshotImage
    UIGraphicsBeginImageContextWithOptions(image[1].size,NO,[UIScreen mainScreen].scale);
    [image[1] drawInRect:CGRectMake(0, 0, image[1].size.width, image[1].size.height)];
    [image[0] drawInRect:CGRectMake(0, 0, image[0].size.width, image[0].size.height)];
    UIImage *screenshotImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return screenshotImage;
}
@end


@interface FlutterScreenshotListenerPlugin ()
{
    NSNotification *_screenshotNotification;
    FlutterMethodChannel *_channel;
    FlutterResult _result;
}
@end

@implementation FlutterScreenshotListenerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                methodChannelWithName:@"com.mysoft/screenshot_listener"
                                     binaryMessenger:[registrar messenger]];
    FlutterScreenshotListenerPlugin* instance = [[FlutterScreenshotListenerPlugin alloc] initWithRegistrar:registrar withChannel:channel];
    [registrar addMethodCallDelegate:instance channel:channel];
}
- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar withChannel:(FlutterMethodChannel *)channel
{
    self = [super init];
    _channel = channel;
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {

    _result = result;
    NSString *methodName = call.method;
    if ([methodName isEqualToString:@"register"]) {

        [self monitorScreenshot];
        result(nil);
    }else if ([methodName isEqualToString:@"unregister"]){
        [self removeMonitorScreenshot];
        result(nil);
    }
    else {
        result(FlutterMethodNotImplemented);
    }
}

//监听截屏
- (void)monitorScreenshot
{
    if (!_screenshotNotification) {

        _screenshotNotification = [NSNotification notificationWithName:UIApplicationUserDidTakeScreenshotNotification object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(userDidTakeScreenshot:) name:UIApplicationUserDidTakeScreenshotNotification object:nil];
    }
}


- (void)userDidTakeScreenshot:(NSNotification *)notification
{

    if ([notification.name isEqualToString:UIApplicationUserDidTakeScreenshotNotification]) {

        NSLog(@"截屏事件发生");
        [self screenshot];
    }
}

//移除截屏监听
- (void)removeMonitorScreenshot
{
    if (_screenshotNotification) {

        _screenshotNotification = nil;
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationUserDidTakeScreenshotNotification object:nil];
}


- (void)screenshot
{

    NSString *path = [NSTemporaryDirectory() stringByAppendingPathComponent:@"screenshot.png"];
    UIWindow *keyWindow = [UIApplication sharedApplication].keyWindow;
    UIImage *ima = [keyWindow screenshot];
    NSInteger width = ima.size.width*[UIScreen mainScreen].scale;
    NSInteger height = ima.size.height*[UIScreen mainScreen].scale;
    float ratio = 0.8;
    CGSize targetSize = CGSizeMake(width, height);
    ima = [self imageWithImageSimple:ima scaledToSize:targetSize];
    NSFileManager *fm = [NSFileManager defaultManager];
    if ([fm fileExistsAtPath:path isDirectory:nil]) {
        [fm removeItemAtPath:path error:nil];
    }
    __block BOOL success = NO;
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    dispatch_group_t group = dispatch_group_create();
    dispatch_group_async(group, queue, ^{

        NSData *imgData = UIImageJPEGRepresentation(ima, ratio);
        success = [imgData writeToFile:path atomically:YES];
    });
    dispatch_group_notify(group, dispatch_get_main_queue(), ^{

        NSString *arg = path;
        if (!success) {

            arg = @"";
        }
        [self->_channel invokeMethod:@"onScreenshot" arguments:arg];
    });
}


/**
 * 压缩成指定大小
 */
- (UIImage*)imageWithImageSimple:(UIImage*)sourceImage scaledToSize:(CGSize)newSize
{
    UIGraphicsBeginImageContextWithOptions(newSize, YES, 1.0);
    CGRect imageRect = CGRectMake(0, 0,newSize.width, newSize.height);
    [sourceImage drawInRect:imageRect];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return newImage;

}

@end

