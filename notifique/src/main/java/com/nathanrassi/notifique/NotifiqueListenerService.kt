package com.nathanrassi.notifique

import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.android.AndroidInjection
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class NotifiqueListenerService : NotificationListenerService() {
  @Inject internal lateinit var dao: Notifique.Dao
  private val store = NotificationStore()

  override fun onCreate() {
    AndroidInjection.inject(this)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val notificationId = sbn.id
    if (store.addNotificationId(packageName, notificationId)) {
      val notificationExtras = sbn.notification.extras
      val message = notificationExtras.getCharSequence(EXTRA_TEXT)
      val title = notificationExtras.getCharSequence(EXTRA_TITLE)
      if (message != null && title != null) {
        val notification = Notifique(
            message.toString(), title.toString(), packageName,
            sbn.postTime
        )
        launch { dao.insert(notification) }
      }
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val notificationId = sbn.id
    store.removeNotificationId(packageName, notificationId)
  }
}

internal class NotificationStore {
  private val packageNameToIds = mutableMapOf<String, MutableList<Int>>()

  fun addNotificationId(
    pkg: String,
    id: Int
  ): Boolean {
    val notificationIdList = packageNameToIds[pkg]
    if (notificationIdList != null) {
      if (notificationIdList.contains(id)) {
        return false
      }
      notificationIdList += id
      return true
    }
    packageNameToIds[pkg] = mutableListOf(id)
    return true
  }

  fun removeNotificationId(
    pkg: String,
    id: Int
  ) {
    // Null or false iff the notification was posted before the listener service was enabled.
    packageNameToIds[pkg]?.remove(id)
  }
}
