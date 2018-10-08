package tl.com.ancs_service.busbean;

import android.app.Notification;

/**
 * Created by tl on 2018-9-27
 */
public class NotificationRemovedBusBean {

  private Notification notification;

  public NotificationRemovedBusBean(Notification notification) {
    this.notification = notification;
  }

  public Notification getNotification() {
    return notification;
  }

  public void setNotification(Notification notification) {
    this.notification = notification;
  }
}
