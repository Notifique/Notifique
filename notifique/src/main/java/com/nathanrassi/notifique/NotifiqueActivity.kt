package com.nathanrassi.notifique

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import com.nathanrassi.notifique.NotifiqueListView.OnSelectionStateChangedListener
import dagger.android.AndroidInjection
import java.util.ArrayDeque
import javax.inject.Inject

class NotifiqueActivity : AppCompatActivity() {
  @Inject internal lateinit var appComponent: AppComponent
  private var snackbar: Snackbar? = null
  var backStack = ArrayDeque<String>()

  override fun onCreate(savedInstanceState: Bundle?) {
    AndroidInjection.inject(this)
    super.onCreate(savedInstanceState)
    val view = findViewById<ViewGroup>(android.R.id.content)
    checkNotificationPermission()
    val inflater = LayoutInflater.from(withAppComponent(appComponent))
    inflater.inflate(R.layout.main, view, true)
    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    toolbar.inflateMenu(R.menu.toolbar)
    val list = findViewById<NotifiqueListView>(R.id.list)
    val deleteButton = toolbar.menu.findItem(R.id.delete)
    val deselectButton = toolbar.menu.findItem(R.id.deselect)
    val searchItem = toolbar.menu.findItem(R.id.search_notifiques)
    val searchView = searchItem?.actionView as SearchView
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        val inputMethodManager = getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        if (query != null) {
          list.searchNotifiques(query)
          backStack.addLast(query)
        }
        return false
      }
      override fun onQueryTextChange(query: String?): Boolean {
        return true
      }
    })

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
        R.id.search_notifiques -> {
          searchView.isIconified = false
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
        searchItem.isVisible = !selected
      }
    }
  }

  override fun onBackPressed() {
    val list = findViewById<NotifiqueListView>(R.id.list)
    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    val searchItem = toolbar.menu.findItem(R.id.search_notifiques)
    val searchView = searchItem?.actionView as SearchView
    if (!backStack.isEmpty()) {
      if (backStack.size == 1) {
        backStack.pollLast()
        list.allNotifiqueView()
        searchItem.collapseActionView()
        searchView.setQuery("",false)
        return
      } else {
        backStack.pollLast()
        searchView.setQuery(backStack.peekLast(),false)
        list.searchNotifiques(backStack.peekLast()!!)
        return
      }
    }
    else if (searchItem.isActionViewExpanded) {
      searchItem.collapseActionView()
      return
    } else {
      val startMain = Intent(Intent.ACTION_MAIN)
      startMain.addCategory(Intent.CATEGORY_HOME)
      startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      startActivity(startMain)
      return
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
