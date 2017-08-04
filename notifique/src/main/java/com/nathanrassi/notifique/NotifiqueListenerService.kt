package com.nathanrassi.notifique

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueListenerService : NotificationListenerService() {
  @Inject internal lateinit var dao: Notifique.Dao
  val TAG: String = "Notifique"
  val notificationMap = mutableMapOf<String,MutableList<Int>>()

  override fun onCreate() {
    AndroidInjection.inject(this)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName: String = sbn.packageName.toString()
    val notificationID: Int = sbn.id
    if (uniqueNotification(packageName, notificationID)) {
      //create notification object for db call
      val message: String? = sbn.notification.extras.getString(android.app.Notification.EXTRA_TEXT).toString()
      val title: String? = sbn.notification.extras.getString(android.app.Notification.EXTRA_TITLE).toString()
      if (!message.isNullOrEmpty() && !title.isNullOrEmpty()) {
        val notification: Notifique = Notifique(message, title, packageName, sbn.postTime)
        dao.insert(notification)
      } else {
        Log.e(TAG, "message or title null/empty, nothing to add")
      }
    }

  }

  override fun onNotificationRemoved(sbn: StatusBarNotification) {
    val packageName: String = sbn.packageName.toString()
    val notificationID: Int = sbn.id
    removeNotification(packageName, notificationID)
  }


  private fun uniqueNotification(pkg: String, id: Int): Boolean{
    if (notificationMap.containsKey(pkg)) {
      if(notificationMap[pkg]!!.contains(id)) {
        return false
      } else {
        notificationMap[pkg]!!.plusAssign(id)
        return true
      }
    } else {
      notificationMap.put(pkg, mutableListOf(id))
      return true
    }
  }

  private fun removeNotification(pkg: String, id: Int) {
    //has to have the pkg at this point????
    if (notificationMap.containsKey(pkg)) {
      if(notificationMap[pkg]!!.contains(id)) {
        notificationMap[pkg]!!.remove(id)
      }
      if (notificationMap[pkg]!!.isEmpty()) {
        notificationMap.remove(pkg)
      }
    }
  }



}
