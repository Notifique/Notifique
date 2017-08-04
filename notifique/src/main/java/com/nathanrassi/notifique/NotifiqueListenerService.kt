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
      val message = notificationExtras.getString(EXTRA_TEXT).toString()
      val title = notificationExtras.getString(EXTRA_TITLE).toString()
      if (!message.isNullOrEmpty() && !title.isNullOrEmpty()) {
        val notification: Notifique = Notifique(message, title, packageName, sbn.postTime)
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
      } else {
        notificationIdList.plusAssign(id)
        return true
      }
    } else {
      packageNameToIds.put(pkg, mutableListOf(id))
      return true
    }
  }

  private fun removeNotificationId(pkg: String, id: Int) {
    val notificationIdList = packageNameToIds[pkg]
    //This if is used for notifications that are present before listen service is enabled.
    if (notificationIdList != null) {
      if (notificationIdList.contains(id)) {
        notificationIdList.remove(id)
      }
      if (notificationIdList.isEmpty()) {
        packageNameToIds.remove(pkg)
      }
    }
  }
}
