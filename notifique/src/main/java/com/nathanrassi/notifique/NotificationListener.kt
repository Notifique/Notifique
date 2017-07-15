package com.nathanrassi.notifique

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener() : NotificationListenerService() {

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val TAG: String = "Notifique"
    var sbnString = sbn.toString()
    Log.v(TAG, sbnString)
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification) {

  }


}
