package com.nathanrassi.notifique

import android.support.v7.app.AppCompatActivity
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NotificationManagerCompat

class NotifiqueActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.notifique_activity)
    if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(this.packageName)) {
      //send to notification settings
      val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
      this.startActivity(intent)
    }
  }

}

