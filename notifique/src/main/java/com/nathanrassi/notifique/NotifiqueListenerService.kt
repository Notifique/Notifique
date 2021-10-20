package com.nathanrassi.notifique

import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.android.AndroidInjection
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NotifiqueListenerService : NotificationListenerService() {
  @Inject internal lateinit var savedNotifiqueQueries: SavedNotifiqueQueries
  private val store = NotificationStore()

  override fun onCreate() {
    AndroidInjection.inject(this)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val notificationId = sbn.id
    val appName =
      packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
        .toString()
    if (store.addNotificationId(packageName, notificationId)) {
      val key = sbn.key
      val notificationExtras = sbn.notification.extras
      val title = notificationExtras.getCharSequence(EXTRA_TITLE)
      val message = notificationExtras.getCharSequence(EXTRA_TEXT)
      if (message != null && title != null) {
        GlobalScope.launch(Dispatchers.IO) {
          savedNotifiqueQueries.insert(
            key,
            title.toString(),
            message.toString(),
            appName,
            packageName,
            sbn.postTime
          )
        }
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
