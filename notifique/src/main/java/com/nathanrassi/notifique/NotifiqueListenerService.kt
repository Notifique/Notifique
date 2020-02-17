package com.nathanrassi.notifique

import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotifiqueListenerService : NotificationListenerService() {
  @Inject internal lateinit var notifiqueQueries: NotifiqueQueries
  private lateinit var scope: CoroutineScope
  private val store = NotificationStore()

  override fun onCreate() {
    AndroidInjection.inject(this)
    scope = MainScope()
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    val notificationId = sbn.id
    val appName = getAppName(packageName)
    if (store.addNotificationId(packageName, notificationId)) {
      val notificationExtras = sbn.notification.extras
      val message = notificationExtras.getCharSequence(EXTRA_TEXT)
      val title = notificationExtras.getCharSequence(EXTRA_TITLE)
      if (message != null && title != null) {
        scope.launch(Dispatchers.IO) {
          android.util.Log.d("eric", "eric insert " + Thread.currentThread())
          notifiqueQueries.insert(
              message.toString(), title.toString(), appName, packageName, sbn.postTime
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

  fun getAppName(pkgNme: String): String {
    var packageManager = applicationContext.getPackageManager()
    var existingAppName =
      packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgNme, 0))
          .toString()
    return existingAppName
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
