package com.nathanrassi.notifique

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build.VERSION.SDK_INT
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

internal class DebugView(
  context: Context,
  attributes: AttributeSet
) : ScrollView(context, attributes) {
  @Inject @NotificationIdProvider lateinit var notificationIdProvider: AtomicInteger
  @Inject lateinit var databaseDelayer: DatabaseDelayer
  private val channelId = "debug"

  init {
    (context.appComponent as DebugAppComponent).inject(this)

    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (SDK_INT >= 26) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          channelId,
          context.getText(R.string.debug_notifications_channel_name),
          IMPORTANCE_LOW
        )
      )
    }

    LayoutInflater.from(context)
      .inflate(R.layout.debug_view_children, this, true)
    findViewById<View>(R.id.send_two_notifications).setOnClickListener {
      notificationManager.postNotification(
        context.getText(R.string.notification_1_title),
        context.getText(R.string.notification_1_message)
      )
      notificationManager.postNotification(
        context.getText(R.string.notification_2_title),
        context.getText(R.string.notification_2_message)
      )
    }
    findViewById<EditText>(
      R.id.database_delay
    ).apply {
      setText(databaseDelayer.databaseDelayMillis.toString())
      addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
          databaseDelayer.databaseDelayMillis = if (s.isEmpty()) 0L else s.toString().toLong()
        }

        override fun beforeTextChanged(
          s: CharSequence,
          start: Int,
          count: Int,
          after: Int
        ) {
          // No-op.
        }

        override fun onTextChanged(
          s: CharSequence,
          start: Int,
          before: Int,
          count: Int
        ) {
          // No-op.
        }
      })
    }
  }

  private fun NotificationManager.postNotification(
    title: CharSequence,
    message: CharSequence
  ) {
    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(BigTextStyle().bigText(message))
      .build()
    notify(notificationIdProvider.incrementAndGet(), notification)
  }

  override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
    super.onApplyWindowInsets(insets)
    // Return non-consumed insets.
    return insets
  }
}
