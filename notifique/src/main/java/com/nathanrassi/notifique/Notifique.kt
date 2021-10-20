package com.nathanrassi.notifique

import android.app.PendingIntent
import android.os.Parcel
import android.os.Parcelable

internal data class Notifique(
  val savedId: Long,
  val title: String,
  val message: String,
  val app: String,
  val packageName: String,
  val timestamp: Long,
  val contentIntent: PendingIntent?
) : Parcelable {
  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeLong(savedId)
    parcel.writeString(title)
    parcel.writeString(message)
    parcel.writeString(app)
    parcel.writeString(packageName)
    parcel.writeLong(timestamp)
    if (contentIntent == null) {
      parcel.writeInt(0)
    } else {
      parcel.writeInt(1)
      contentIntent.writeToParcel(parcel, flags)
    }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<Notifique> {
    fun SavedNotifique.asNotifique(notificationContentIntents: Map<String, PendingIntent>): Notifique {
      return Notifique(
        id,
        title,
        message,
        app,
        packageName,
        timestamp,
        notificationContentIntents[key]
      )
    }

    override fun createFromParcel(parcel: Parcel): Notifique {
      val savedId = parcel.readLong()
      val title = parcel.readString()!!
      val message = parcel.readString()!!
      val app = parcel.readString()!!
      val packageName = parcel.readString()!!
      val timestamp = parcel.readLong()
      val contentIntent = if (parcel.readInt() == 0) {
        null
      } else {
        PendingIntent.CREATOR.createFromParcel(parcel)
      }
      return Notifique(
        savedId,
        title,
        message,
        app,
        packageName,
        timestamp,
        contentIntent
      )
    }

    override fun newArray(size: Int): Array<Notifique?> {
      return arrayOfNulls(size)
    }
  }
}
