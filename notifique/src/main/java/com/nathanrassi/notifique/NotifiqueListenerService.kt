package com.nathanrassi.notifique

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueListenerService : NotificationListenerService() {
  @Inject internal lateinit var dao: Notifique.Dao

  override fun onCreate() {
    AndroidInjection.inject(this)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification) {
  }
}
