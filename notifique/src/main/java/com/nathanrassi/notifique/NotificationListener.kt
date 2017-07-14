package com.nathanrassi.notifique

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener() : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }


}
