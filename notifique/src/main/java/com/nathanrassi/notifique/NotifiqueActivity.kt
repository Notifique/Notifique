package com.nathanrassi.notifique

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueActivity : AppCompatActivity() {
  @Inject internal lateinit var appComponent: AppComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    val view = findViewById<ViewGroup>(android.R.id.content)
    checkNotificationPermission()
    val inflater = LayoutInflater.from(withAppComponent(appComponent))
    inflater.inflate(R.layout.main, view, true)
  }

  override fun onResume() {
    super.onResume()
    checkNotificationPermission()
  }

  private fun checkNotificationPermission() {
    if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
      val notificationSnackbar = Snackbar.make(
          findViewById(android.R.id.content),
          R.string.snackbar,
          Snackbar.LENGTH_INDEFINITE
      )
      notificationSnackbar.setAction(R.string.snackbar_action) {
        val intent = Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (packageManager.queryIntentActivities(intent, 0).isEmpty()) {
          // TODO: rare problem.
        } else {
          startActivity(intent)
        }
      }
      notificationSnackbar.show()
    }
  }
}
