package com.nathanrassi.notifique

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.ComponentName
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build.VERSION.SDK_INT
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.DEFAULT_ALL
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import okio.appendingSink
import okio.buffer

/**
 * DiskCrashReporter writes [Throwable]s to the disk for viewing later and posts a notification.
 */
internal class DiskCrashReporter @Inject constructor(
  private val application: Application,
  @NotificationIdProvider private val notificationIdProvider: AtomicInteger
) : CrashReporter {
  private val channelId = "crash_reporter"

  override fun report(cause: Throwable) {
    val message = cause.message

    val notificationBuilder = NotificationCompat.Builder(application, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(application.getText(R.string.crash_report_notification_title))
      .setContentText(message)
      .setDefaults(DEFAULT_ALL)

    var report: String
    val externalFilesDirectory = application.getExternalFilesDir(null)
    if (externalFilesDirectory == null) {
      report = "Failed to write to disk. externalFilesDirectory == null"
    } else {
      val file = File(externalFilesDirectory, "crash_reports.txt")
      try {
        file.appendingSink()
          .buffer()
          .use { sink ->
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormat.timeZone = TimeZone.getDefault()
            sink.writeUtf8(dateFormat.format(Date()))
            sink.writeUtf8("\n")
            cause.printStackTrace(PrintStream(sink.outputStream()))
            sink.writeUtf8("\n\n")
          }

        report = "Written to disk."

        val textFileViewer = Intent(ACTION_VIEW).apply {
          val authority = "${application.packageName}.fileprovider"
          val data = FileProvider.getUriForFile(application, authority, file)
          setDataAndType(data, "text/plain")
          addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (application.packageManager.hasMatchingActivity(textFileViewer)) {
          notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
              application,
              0,
              textFileViewer,
              FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
            )
          )

          if (SDK_INT >= 25) {
            val shortcutManager = application.getSystemService(ShortcutManager::class.java)

            val mainIntent = Intent(ACTION_MAIN)
              .addCategory(Intent.CATEGORY_LAUNCHER)
              .setPackage(application.packageName)
            val activities = application.packageManager.queryIntentActivities(mainIntent, 0)
            val firstMainActivity = activities.first().activityInfo
            val componentName =
              ComponentName(firstMainActivity.packageName, firstMainActivity.name)
            val shortcutCount = shortcutManager.dynamicShortcuts.count { shortcutInfo ->
              shortcutInfo.activity == componentName
            } + shortcutManager.manifestShortcuts.count { shortcutInfo ->
              shortcutInfo.activity == componentName
            }

            if (shortcutCount < shortcutManager.maxShortcutCountPerActivity) {
              val shortcutInfo = ShortcutInfo.Builder(application, "crash_reports")
                .setActivity(componentName)
                .setShortLabel(application.getText(R.string.crash_report_shortcut_label_short))
                .setLongLabel(application.getText(R.string.crash_report_shortcut_label_long))
                .setIcon(Icon.createWithResource(application, R.mipmap.ic_launcher))
                .setIntent(textFileViewer)
                .build()
              shortcutManager.addDynamicShortcuts(listOf(shortcutInfo))
            }
          }
        }
      } catch (e: IOException) {
        report = "Failed to write to disk. ${e.message}"
      }
    }

    notificationBuilder.setStyle(BigTextStyle().bigText("$report\n$message"))
    val notificationManager =
      application.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (SDK_INT >= 26) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          channelId,
          application.getText(R.string.crash_report_notifications_channel_name),
          IMPORTANCE_HIGH
        )
      )
    }
    notificationManager.notify(
      notificationIdProvider.incrementAndGet(),
      notificationBuilder.build()
    )
  }

  private fun PackageManager.hasMatchingActivity(intent: Intent): Boolean {
    return queryIntentActivities(intent, 0).isNotEmpty()
  }
}
