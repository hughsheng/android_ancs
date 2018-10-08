package tl.com.ancs_service.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.greenrobot.eventbus.EventBus;

import tl.com.ancs_service.busbean.NotificationPostedBusBean;
import tl.com.ancs_service.busbean.NotificationRemovedBusBean;

/**
 * Created by tl on 2018-9-27
 * 通知使用权service用于获取android通知
 */
public class NotificationService extends NotificationListenerService {

  // 通知被移除时回调
  @Override
  public void onNotificationRemoved(StatusBarNotification sbn) {
    super.onNotificationRemoved(sbn);
    Notification notification = sbn.getNotification();
    EventBus.getDefault().post(new NotificationRemovedBusBean(notification));
  }

  // 增加一条通知时回调
  @Override
  public void onNotificationPosted(StatusBarNotification sbn) {
    super.onNotificationPosted(sbn);
    Notification notification = sbn.getNotification();
    EventBus.getDefault().post(new NotificationPostedBusBean(notification));
  }


}
