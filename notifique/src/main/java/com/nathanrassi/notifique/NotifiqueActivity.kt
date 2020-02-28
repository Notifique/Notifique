package com.nathanrassi.notifique

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import com.nathanrassi.notifique.NotifiqueListView.OnSelectionStateChangedListener
import dagger.android.AndroidInjection
import javax.inject.Inject

class NotifiqueActivity : AppCompatActivity() {
  @Inject internal lateinit var appComponent: AppComponent
  private var snackbar: Snackbar? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    val view = findViewById<ViewGroup>(android.R.id.content)
    checkNotificationPermission()
    val inflater = LayoutInflater.from(withAppComponent(appComponent))
    inflater.inflate(R.layout.main, view, true)
    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    val list = findViewById<NotifiqueListView>(R.id.list)
    toolbar.inflateMenu(R.menu.toolbar)
    val deleteButton = toolbar.menu.findItem(R.id.delete)
    val deselectButton = toolbar.menu.findItem(R.id.deselect)
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.delete -> {
          list.deleteSelected()
          true
        }
        R.id.deselect -> {
          list.deselectSelected()
          true
        }
        else -> throw AssertionError()
      }
    }
    list.onSelectionStateChangedListener = object :
        OnSelectionStateChangedListener {
      override fun onSelectionStateChanged(selected: Boolean) {
        deleteButton.isVisible = selected
        deselectButton.isVisible = selected
      }
    }
  }

  override fun onResume() {
    super.onResume()
    checkNotificationPermission()
  }

  private fun checkNotificationPermission() {
    val snackbar = snackbar
    if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)) {
      if (snackbar?.isShown == true) {
        return
      }
      val intent = Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
      val missingActionActivity = packageManager.queryIntentActivities(intent, 0)
          .isEmpty()
      this.snackbar = Snackbar.make(
          findViewById(android.R.id.content),
          if (missingActionActivity) R.string.snackbar_missing_action else R.string.snackbar,
          LENGTH_INDEFINITE
      )
          .apply {
            if (!missingActionActivity) {
              setAction(R.string.snackbar_action) {
                startActivity(intent)
              }
            }
            setTextColor(getColor(R.color.snackbar_text))
            setActionTextColor(getColor(R.color.snackbar_action_text))
            show()
          }
    } else {
      if (snackbar?.isShown == true) {
        snackbar.dismiss()
        return
      }
    }
  }
}
