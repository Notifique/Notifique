package com.nathanrassi.notifique

import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueListenerService : NotificationListenerService() {
  @Inject internal lateinit var dao: Notifique.Dao
  private val packageNameToIds = mutableMapOf<String, MutableList<Int>>()

  override fun onCreate() {
    AndroidInjection.inject(this)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val notificationId = sbn.id
    if (storeIfUnique(packageName, notificationId)) {
      val notificationExtras = sbn.notification.extras
      val message = notificationExtras.getCharSequence(EXTRA_TEXT)
      val title = notificationExtras.getCharSequence(EXTRA_TITLE)
      if (message != null && title != null) {
        val notification: Notifique = Notifique(message.toString(), title.toString(), packageName,
            sbn.postTime)
        dao.insert(notification)
      }
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val notificationId = sbn.id
    removeNotificationId(packageName, notificationId)
  }

  private fun storeIfUnique(pkg: String, id: Int): Boolean {
    val notificationIdList = packageNameToIds[pkg]
    if (notificationIdList != null) {
      if (notificationIdList.contains(id)) {
        return false
      }
      notificationIdList += id
      return true
    }
    packageNameToIds.put(pkg, mutableListOf(id))
    return true
  }

  private fun removeNotificationId(pkg: String, id: Int) {
    val notificationIdList = packageNameToIds[pkg]
    if (notificationIdList != null) { // Null iff the notification was posted before the listener service was enabled.
      if (!notificationIdList.remove(id)) {
        throw AssertionError(
            "The notification id is always added to the list in onNotificationPosted.")
      }
      if (notificationIdList.isEmpty()) {
        packageNameToIds.remove(pkg)
      }
    }
  }
}
