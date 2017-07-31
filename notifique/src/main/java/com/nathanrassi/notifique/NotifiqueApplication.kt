package com.nathanrassi.notifique

import android.app.Application
import android.arch.persistence.room.Room


class NotifiqueApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    val database = Room.databaseBuilder(this, Database::class.java, "database").build()
  }
}
