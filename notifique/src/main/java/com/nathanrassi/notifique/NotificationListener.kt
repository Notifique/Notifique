package com.nathanrassi.notifique

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener() : NotificationListenerService() {
    override fun onBind(intent: Intent): IBinder {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }


}
