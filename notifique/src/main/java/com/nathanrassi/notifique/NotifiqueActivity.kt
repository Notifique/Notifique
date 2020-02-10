package com.nathanrassi.notifique

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueActivity : AppCompatActivity() {
  @Inject internal lateinit var appComponent: AppComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    val view = findViewById<ViewGroup>(android.R.id.content)


    //Enables Notification Listener Service
    var listenerPackageName = "com.nathanrassi.notifique.NotifiqueListenerService"
    if (Settings.Secure.getString(contentResolver, "enabled_notification_listeners").contains(listenerPackageName)){
      Toast.makeText(this,"They work!", LENGTH_LONG).show()
    } else {
      class SnackbarNotificationListener : View.OnClickListener {
        override fun onClick(v: View) {
          startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
      }
      val notificationSnackbar = Snackbar.make(view, "Please Enable Notification Access", Snackbar.LENGTH_INDEFINITE)
      notificationSnackbar.setAction("Enable Notifications", SnackbarNotificationListener())
      notificationSnackbar.show()
      Toast.makeText(this,"They dont work!", LENGTH_LONG).show()
    }


    val inflater = LayoutInflater.from(withAppComponent(appComponent))
    inflater.inflate(R.layout.list, view, true)
  }
}
