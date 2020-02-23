package com.nathanrassi.notifique

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build.VERSION.SDK_INT
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.ScrollView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import javax.inject.Inject

internal class DebugView(
  context: Context,
  attributes: AttributeSet
) : ScrollView(context, attributes) {
  @Inject lateinit var notificationIdProvider: NotificationIdProvider
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
    notify(notificationIdProvider.notificationId, notification)
  }

  override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
    super.onApplyWindowInsets(insets)
    // Return non-consumed insets.
    return insets
  }
}
