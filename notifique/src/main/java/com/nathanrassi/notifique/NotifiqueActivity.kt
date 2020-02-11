package com.nathanrassi.notifique

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueActivity : AppCompatActivity() {
  @Inject internal lateinit var appComponent: AppComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    val view = findViewById<ViewGroup>(android.R.id.content)
    verifyPriviledge()
    val inflater = LayoutInflater.from(withAppComponent(appComponent))
    inflater.inflate(R.layout.list, view, true)
  }

  override fun onResume() {
    super.onResume()
    verifyPriviledge()
  }


  fun verifyPriviledge() {
    var listenerPackageName = "com.nathanrassi.notifique.NotifiqueListenerService"
    if (Settings.Secure.getString(contentResolver, "enabled_notification_listeners").contains(listenerPackageName)){
    } else {
      class SnackbarNotificationListener : View.OnClickListener {
        override fun onClick(v: View) {
          startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
      }
      val view = findViewById<ViewGroup>(android.R.id.content)
      val notificationSnackbar = Snackbar.make(view, "Please Enable Notification Access", Snackbar.LENGTH_INDEFINITE)
      notificationSnackbar.setAction("Enable Notifications", SnackbarNotificationListener())
      notificationSnackbar.show()
    }
  }
}
