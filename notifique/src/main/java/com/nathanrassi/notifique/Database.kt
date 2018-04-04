package com.nathanrassi.notifique

import android.arch.persistence.room.RoomDatabase

@android.arch.persistence.room.Database(
    entities = [Notifique::class],
    version = 1,
    exportSchema = false
)
internal abstract class Database : RoomDatabase() {
  abstract fun notifiqueDao(): Notifique.Dao
}
